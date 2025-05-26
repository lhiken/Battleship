package map.gen;

import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.annotation.Rpc;
import godot.annotation.RpcMode;
import godot.annotation.Sync;
import godot.annotation.TransferMode;
import godot.api.ArrayMesh;
import godot.api.BaseMaterial3D.ShadingMode;
import godot.api.FastNoiseLite;
import godot.api.FastNoiseLite.NoiseType;
import godot.api.GeometryInstance3D.ShadowCastingSetting;
import godot.api.Mesh.PrimitiveType;
import godot.api.MeshInstance3D;
import godot.api.Node3D;
import godot.api.ORMMaterial3D;
import godot.api.PackedScene;
import godot.api.ResourceLoader;
import godot.api.SurfaceTool;
import godot.core.Color;
import godot.core.Vector2;
import godot.core.Vector3;
import godot.global.GD;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import multiplayer.MultiplayerManager;

// enum to keep track of tile types and their costs
// costs are for pathfinding, nodepaths are to get the right
// tile when spawning them
enum Tile {
    GrassTile(10, false, "GrassTile1"),
    RockTile(10, false, "RockTile"),
    GrassProp(10, false, "GrassProp"),
    RockProp(10, false, "RockProp"),
    SeaMine(5, true, "SeaMine"),
    Shore(4, true, "Shore"),
    Empty(1, true, "Empty"),
    Seaweed(1, true, "Seaweed");

    public final int cost;
    public final boolean walkable;
    public final String nodePath;

    private Tile(int cost, boolean walkable, String nodePath) {
        this.cost = cost;
        this.walkable = walkable;
        this.nodePath = nodePath;
    }
}

/** generator
 * handles map generation and stuff
 * ts is actually really simple and lowk kinda lame
 * my favorite class :heart:
 */
@RegisterClass
public class Generator extends Node3D {

    private static final GD gd = GD.INSTANCE;

    // change this
    private final double cellWidth = 2.0;

    // do not change this
    private final double tileWidth = 2.0;

    private final int mapWidth = 70;
    private final int mapHeight = 70;

    private GridCell[][] grid;

    private FastNoiseLite baseNoise;

    private int noiseSeed = (int) (Math.random() * 1000);
    private float noiseFreq = 0.06f;

    private Queue<Node3D> spawnedTiles;

    private MeshInstance3D debugMesh;

    /** _ready
     * godot built in function, runs on spawn
     * generates the map
     */
    @RegisterFunction
    @Override
    public void _ready() {
        if (getMultiplayer().isServer()) {
            grid = new GridCell[mapWidth][mapHeight];
            spawnedTiles = new LinkedList<>();
            populateGrid();
        }
    }

    /** _process
     * godot built in function, runs every frame
     * adds new child every frame if there are tiles that have not spawned
     */
    @RegisterFunction
    @Override
    public void _process(double delta) {
        if (getMultiplayer().isServer() && !spawnedTiles.isEmpty()) {
            getParent().getNode("MapTiles").addChild(spawnedTiles.poll(), true);
            getParent().getNode("MapTiles").addChild(spawnedTiles.poll(), true);
            getParent().getNode("MapTiles").addChild(spawnedTiles.poll(), true);
            getParent().getNode("MapTiles").addChild(spawnedTiles.poll(), true);
        }
    }

    /** populateGrid
     * populates the grid array with representations of map tiles
     */
    private void populateGrid() {
        // generate noise
        baseNoise = new FastNoiseLite();
        baseNoise.setNoiseType(NoiseType.TYPE_SIMPLEX);
        baseNoise.setSeed(noiseSeed);
        baseNoise.setFrequency(noiseFreq);

        // populate with tiles based on noise and stuff
        for (int x = 0; x < mapWidth; x++) {
            for (int z = 0; z < mapHeight; z++) {
                // oh my god theyre all the same length!
                double height = sampleNoise(baseNoise, x, z);
                Coordinate coord = getCoord(x, z);
                Tile tileType = getTileType(x, z);

                GridCell cell = new GridCell(coord, tileType, height);
                if (height < 0.2) cell.setTile(Tile.Empty);
                cell.setHeight(height * 10 - 1.5);
                grid[x][z] = cell;
            }
        }

        removeCircle(35, 35, 10);

        coordToGrid(new Vector2(0, 0)).setTile(Tile.Shore);
        for (int x = 0; x < mapWidth; x++) {
            for (int z = 0; z < mapHeight; z++) {
                applyShore(x, z);
            }
        }

        FastNoiseLite decoNoise = new FastNoiseLite();

        decoNoise.setNoiseType(NoiseType.TYPE_SIMPLEX);
        decoNoise.setSeed(noiseSeed + 10);
        decoNoise.setFrequency(0.1f);
        for (int x = 0; x < mapWidth; x++) {
            for (int z = 0; z < mapHeight; z++) {
                double val = sampleNoise(decoNoise, x, z);

                if (grid[x][z].getTile() == Tile.Empty && val > 0.4) {
                    grid[x][z].setTile(Tile.Seaweed);
                    grid[x][z].setHeight(Math.random() * 1.0 - 1.0);
                }
            }
        }

        for (int x = 0; x < mapWidth; x++) {
            for (int z = 0; z < mapHeight; z++) {
                spawnTile(grid[x][z]);
            }
        }
    }

    private void removeCircle(int cx, int cz, double radius) {
        int startX = (int) Math.max(0, cx - radius);
        int endX = (int) Math.min(mapWidth - 1, cx + radius);
        int startZ = (int) Math.max(0, cz - radius);
        int endZ = (int) Math.min(mapHeight - 1, cz + radius);

        for (int x = startX; x <= endX; x++) {
            for (int z = startZ; z <= endZ; z++) {
                double dx = x - cx;
                double dz = z - cz;
                double distance = Math.sqrt(dx * dx + dz * dz);

                if (distance <= radius) {
                    GridCell cell = grid[x][z];

                    if (!cell.getTile().walkable) {
                        cell.setTile(Tile.Empty);
                        cell.setHeight(-1.5);
                        spawnTile(cell);
                    } else if (distance > radius - 1.5) {
                        double softFactor = (radius - distance) / 1.5;
                        double newHeight = cell.getHeight() - softFactor * 2.0;
                        cell.setHeight(newHeight);
                        spawnTile(cell);
                    }
                }
            }
        }
    }

    /** applyShore
     * generates shore for ai pathfinding
     */
    private void applyShore(int x, int z) {
        if (grid[x][z].getTile().walkable) return;

        int[][] directions = {
            { 0, -1 },
            { 0, 1 },
            { -1, 0 },
            { 1, 0 }, // radius 1
            { -1, -1 },
            { -1, 1 },
            { 1, -1 },
            { 1, 1 }, // diagonals
            { 0, -2 },
            { 0, 2 },
            { -2, 0 },
            { 2, 0 }, // straight radius 2
        };

        for (int[] dir : directions) {
            int nx = x + dir[0];
            int nz = z + dir[1];

            if (nx >= 0 && nx < grid.length && nz >= 0 && nz < grid[0].length) {
                if (grid[nx][nz].getTile().walkable) {
                    gd.print("set " + x + " " + z);
                    grid[nx][nz].setTile(Tile.Shore);
                }
            }
        }
    }

    /** sampleNoise
     * samples the noise texture to get tile height
     */
    private double sampleNoise(FastNoiseLite noise, int x, int z) {
        double val = noise.getNoise2d(x, z);
        return val;
    }

    /** getCoord
     * converts array indices into a Coordinate object
     */
    private Coordinate getCoord(int xIndex, int zIndex) {
        // position of each axis, scaled to -w to w and -h to h
        double px = xIndex * cellWidth - (mapWidth * cellWidth) / 2;
        double pz = zIndex * cellWidth - (mapHeight * cellWidth) / 2;
        Coordinate newCoord = new Coordinate(px, pz, xIndex, zIndex);
        return newCoord;
    }

    /** getTileType
     * gets the tile type for the biome
     */
    private Tile getTileType(int x, int z) {
        return Tile.GrassTile;
    }

    /** spawnTile
     * makes the tile exist
     */
    private void spawnTile(GridCell cell) {
        String tileName = cell.getTile().nodePath;
        if (tileName.equals("Empty") || tileName.equals("Shore")) return;
        String tilePath = "res://components/tiles/" + tileName + ".tscn";

        PackedScene tileScene = (PackedScene) ResourceLoader.load(tilePath);
        if (tileScene == null) {
            gd.print("Failed to load tile scene: " + tilePath);
            return;
        }

        Vector3 pos = new Vector3(
            cell.getCoords().getX(),
            cell.getHeight(),
            cell.getCoords().getZ()
        );

        Node3D tileInstance = (Node3D) tileScene.instantiate();
        float scale =
            (float) (1.0f - ((Math.random() * 0.2))) *
            (float) (cellWidth / tileWidth);
        tileInstance.setPosition(pos);
        tileInstance.setRotation(
            new Vector3(
                0,
                (Math.PI / 2.0) * Math.round((Math.random() * 4)) +
                (Math.random() - 0.5) * 1.5,
                0
            )
        );
        tileInstance.setScale(new Vector3(scale, scale, scale));

        spawnedTiles.add(tileInstance);
    }

    /** navigate
     * a* pathfinding, takes a start and end position and generates
     * a path between the two positions
     */
    @Rpc(
        rpcMode = RpcMode.ANY,
        sync = Sync.SYNC,
        transferMode = TransferMode.UNRELIABLE
    )
    @RegisterFunction
    public ArrayList<Coordinate> navigate(Vector3 startPos, Vector3 endPos) {
        long startTime = System.nanoTime();
        // get the grid cells that correspond with the world positions
        Vector2 sPosition = new Vector2(startPos.getX(), startPos.getZ());
        Vector2 ePosition = new Vector2(endPos.getX(), endPos.getZ());
        GridCell start = coordToGrid(sPosition);
        GridCell end = coordToGrid(ePosition);

        // gd.print("began pathfinding\n" + start + " to " + end);

        // a* algorithm 😱

        // the pq is funny because we want to be able to assign each grid cell
        // a custom priority that gets updated throughout the search, but we
        // dont want to do that in gridcell because we want to keep this self
        // contained without affecting anything else
        PriorityQueue<Pair<GridCell, Double>> frontier = new PriorityQueue<>(
            Comparator.comparingDouble(Pair::getSecond) // ive never seen this syntax in java either!
        );
        frontier.add(new Pair<GridCell, Double>(start, 0d));
        HashMap<GridCell, GridCell> cameFrom = new HashMap<>();
        cameFrom.put(start, null);
        HashMap<GridCell, Double> gCost = new HashMap<>();
        gCost.put(start, 0d);

        while (!frontier.isEmpty()) {
            GridCell currentCell = frontier.poll().getFirst();
            if (currentCell == end) break;

            ArrayList<GridCell> currentNeighbors = getNeighbors(
                currentCell.getCoords().getXIndex(),
                currentCell.getCoords().getZIndex()
            );
            for (GridCell next : currentNeighbors) {
                double newCost = gCost.get(currentCell) + getCost(next);
                if (!gCost.containsKey(next) || newCost < gCost.get(next)) {
                    gCost.put(next, newCost);
                    double priority = newCost + getHeuristic(next, end);
                    frontier.add(new Pair<>(next, priority));
                    cameFrom.put(next, currentCell);
                }
            }
        }

        // go backwards from end cell to get path
        ArrayList<Coordinate> path = new ArrayList<>();
        GridCell current = end;

        while (current != start) {
            path.add(current.getCoords());
            current = cameFrom.get(current);
        }

        path.add(start.getCoords());
        Collections.reverse(path);

        long endTime = System.nanoTime();
        long duration = endTime - startTime;

        // gd.print(
        //     cameFrom.size() +
        //     " nodes in " +
        //     (duration / 1_000_000.0) +
        //     " milliseconds"
        // );

        return path;
    }

    /** getHeuristic
     * a* pathfinding heuristic
     */
    private double getHeuristic(GridCell a, GridCell b) {
        int dx = a.getCoords().getXIndex() - b.getCoords().getXIndex();
        int dz = a.getCoords().getZIndex() - b.getCoords().getZIndex();
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** getCost
     * gets cost to move to a tile
     */
    private double getCost(GridCell next) {
        return next.getTile().cost;
    }

    /** coordTogrid
     * takes a Vector2 world position and converts it to a GridCell object
     */
    private GridCell coordToGrid(Vector2 coords) {
        int xi = (int) ((coords.getX() + (mapWidth * cellWidth) / 2) /
            cellWidth);
        int zi = (int) ((coords.getY() + (mapHeight * cellWidth) / 2) /
            cellWidth);
        return grid[xi][zi];
    }

    /** getNeighbors
     * takes an array index and returns the neighbors
     */
    private ArrayList<GridCell> getNeighbors(int x, int z) {
        ArrayList<GridCell> arr = new ArrayList<>(4);
        if (x > 0 && grid[x - 1][z].getTile().walkable) arr.add(grid[x - 1][z]);
        if (x < mapWidth - 1 && grid[x + 1][z].getTile().walkable) arr.add(
            grid[x + 1][z]
        );
        if (z > 0 && grid[x][z - 1].getTile().walkable) arr.add(grid[x][z - 1]);
        if (z < mapHeight - 1 && grid[x][z + 1].getTile().walkable) arr.add(
            grid[x][z + 1]
        );
        return arr;
    }

    /** visualizePath
     * debug function for visualizing a generated path
     */
    public void visualizePath(ArrayList<Coordinate> path) {
        if (debugMesh != null) {
            debugMesh.queueFree();
            debugMesh = null;
        }

        if (path.size() < 2) return;

        MeshInstance3D meshInstance = new MeshInstance3D();
        SurfaceTool surfaceTool = new SurfaceTool();

        surfaceTool.begin(PrimitiveType.LINES);

        for (int i = 0; i < path.size() - 1; i++) {
            surfaceTool.setColor(new Color(0.5, 0.5, 0.5, 0.8));
            surfaceTool.addVertex(path.get(i).toVec3());

            surfaceTool.setColor(new Color(0.5, 0.5, 0.5, 0.8));
            surfaceTool.addVertex(path.get(i + 1).toVec3());
        }

        ArrayMesh mesh = surfaceTool.commit();

        ORMMaterial3D material = new ORMMaterial3D();
        material.setShadingMode(ShadingMode.UNSHADED);
        material.setAlbedo(new Color(0.0, 1.0, 0.0, 1.0));

        meshInstance.setMesh(mesh);
        meshInstance.setMaterialOverride(material);
        meshInstance.setCastShadowsSetting(ShadowCastingSetting.OFF);

        debugMesh = meshInstance;
        addChild(debugMesh);
    }

    @RegisterFunction
    public boolean checkWalkable(Vector3 pos) {
        Vector2 realPos = new Vector2(pos.getX(), pos.getZ());
        return (
            coordToGrid(realPos).getTile().walkable &&
            coordToGrid(realPos).getTile().cost == 1
        );
    }
}

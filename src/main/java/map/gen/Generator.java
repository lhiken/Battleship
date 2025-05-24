package map.gen;

import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.annotation.Rpc;
import godot.annotation.RpcMode;
import godot.annotation.Sync;
import godot.annotation.TransferMode;
import godot.api.FastNoiseLite;
import godot.api.FastNoiseLite.NoiseType;
import godot.api.Node3D;
import godot.api.PackedScene;
import godot.api.ResourceLoader;
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
    Shore(3, true, "Shore"),
    Empty(1, true, "Empty");

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
                double height = sampleNoise(x, z);
                Coordinate coord = getCoord(x, z);
                Tile tileType = getTileType(x, z);

                GridCell cell = new GridCell(coord, tileType, height);
                if (height < 0.2) cell.setTile(Tile.Empty);
                cell.setHeight(height * 10 - 2.0);
                spawnTile(cell);
                grid[x][z] = cell;
                applyShore(x, z);
            }
        }
    }

    /** applyShore
     * generates shore for ai pathfinding
     */
    private void applyShore(int x, int z) {
        if (grid[x][z] == null || grid[x][z].getTile() == Tile.Empty) return;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;

                int nx = x + dx;
                int nz = z + dz;

                if (nx >= 0 && nx < mapWidth && nz >= 0 && nz < mapHeight) {
                    if (
                        grid[nx][nz] == null ||
                        grid[nx][nz].getTile() == Tile.Empty
                    ) {
                        grid[x][z].setTile(Tile.Shore);
                        return;
                    }
                }
            }
        }
    }

    /** sampleNoise
     * samples the noise texture to get tile height
     */
    private double sampleNoise(int x, int z) {
        double val = baseNoise.getNoise2d(x, z);
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
        if (tileName.equals("Empty")) return;
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

        // gd.print(pos);

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
        int dx = Math.abs(
            a.getCoords().getXIndex() - b.getCoords().getXIndex()
        );
        int dz = Math.abs(
            a.getCoords().getZIndex() - b.getCoords().getZIndex()
        );
        return dx + dz;
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
}

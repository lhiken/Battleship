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
import main.MatchManager;
import multiplayer.MultiplayerManager;

/**
 * An enum to keep track of tile types and their costs, which
 * are for pathfinding with nodepaths to get the node path
 * for spawning them
 */
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
 * handles generating the map and provides a pathfinding api
 */
@RegisterClass
public class Generator extends Node3D {

    /**
     *
     */
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
     * godot built-in function, runs on spawn
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

    /**
     * godot built-in function, runs every frame
     * adds new child every frame if there are tiles that have not spawned
     *
     * @param delta provided by godot, time passed since last frame
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

    /**
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
                if (height < 0.25) cell.setTile(Tile.Empty);
                cell.setHeight(height * 10 - 2.0);
                grid[x][z] = cell;
            }
        }

        removeCircle(35, 35, 6);

        MatchManager manager = (MatchManager) getParent();
        for (Vector2 spawn : manager.getSpawnPoints()) {
            gd.print("added spawn");
            Coordinate coord = coordToGrid(spawn).getCoords();
            removeCircle(coord.getXIndex(), coord.getZIndex(), 5);
        }

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

        int spawned = 0;
        while (spawned < 40) {
            int x = (int) Math.round(Math.random() * (mapWidth - 1));
            int z = (int) Math.round(Math.random() * (mapHeight - 1));
            if (grid[x][z].getTile().walkable) {
                grid[x][z].setTile(Tile.SeaMine);
                grid[x][z].setHeight(-0.4);
                spawned++;
            }
        }

        for (int x = 0; x < mapWidth; x++) {
            for (int z = 0; z < mapHeight; z++) {
                spawnTile(grid[x][z]);
            }
        }
    }

    /**
     * a helper method to clear a circle from the world for
     * objects/spawn points
     *
     * @param cx index of center of circle in x
     * @param cz index of center of circle in z
     * @param radius the radius of the circle
     */
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

    /**
     * generates shore tiles for a tile to prevent AI from pathing to land
     * @param x the x index of the tile
     * @param z the z index of the tile
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
                    grid[nx][nz].setTile(Tile.Shore);
                }
            }
        }
    }

    /**
     * samples a noise texture
     * @param noise the noise texture object
     * @param x the x position being sampled
     * @param z the z position being sampled
     * @return the value of the noise at the sampled point
     */
    private double sampleNoise(FastNoiseLite noise, int x, int z) {
        double val = noise.getNoise2d(x, z);
        return val;
    }

    /**
     * converts array indices into a coordinate object
     * @param xIndex x index in array
     * @param zIndex z index in array
     * @return Coordinate object with real positions
     */
    private Coordinate getCoord(int xIndex, int zIndex) {
        // position of each axis, scaled to -w to w and -h to h
        double px = xIndex * cellWidth - (mapWidth * cellWidth) / 2;
        double pz = zIndex * cellWidth - (mapHeight * cellWidth) / 2;
        Coordinate newCoord = new Coordinate(px, pz, xIndex, zIndex);
        return newCoord;
    }

    /**
     * gets the tile type for a particular tile
     *
     * @param x x position
     * @param z z position
     * @return tile type
     */
    private Tile getTileType(int x, int z) {
        return Tile.GrassTile;
    }

    /**
     * spawns the tile into the world
     * @param cell the GridCell to spawn
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
            (float) (1.0f - ((Math.random() * 0.15))) *
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

    /**
     * A* algorithm for pathfinding
     *
     * @param startPos starting position in world space
     * @param endPos ending position in world space
     * @return array of coordinates with path positions, in world space
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

    /**
     * A* pathfinding heuristic, euler distance (pythagorean theorem)
     * @param a start
     * @param b end
     * @return heuristic (how much we want to go there)
     */
    private double getHeuristic(GridCell a, GridCell b) {
        int dx = a.getCoords().getXIndex() - b.getCoords().getXIndex();
        int dz = a.getCoords().getZIndex() - b.getCoords().getZIndex();
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * the cost of a tile
     * @param next gridTile being tested
     * @return the cost of the tile
     */
    private double getCost(GridCell next) {
        return next.getTile().cost;
    }

    /**
     * takes a coordinate vector and gets the associated gridCell
     * @param coords the coordinate in world space
     * @return the associated cell
     */
    private GridCell coordToGrid(Vector2 coords) {
        int xi = (int) ((coords.getX() + (mapWidth * cellWidth) / 2) /
            cellWidth);
        int zi = (int) ((coords.getY() + (mapHeight * cellWidth) / 2) /
            cellWidth);
        return grid[xi][zi];
    }

    /**
     * gets the neighbors of a cell, helper method for A*
     * @param x x index being checked
     * @param z z index being checked
     * @return array of neighboring GridCells
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

    /**
     * checks if a certain position is walkable or has a low cost
     * @param pos position in world space
     * @return whether the tile is walkable/has a low cost or not
     */
    @RegisterFunction
    public boolean checkWalkable(Vector3 pos) {
        Vector2 realPos = new Vector2(pos.getX(), pos.getZ());
        return (
            coordToGrid(realPos).getTile().walkable &&
            coordToGrid(realPos).getTile().cost == 1
        );
    }
}

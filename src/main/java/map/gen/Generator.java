package map.gen;

import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
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

// enum to keep track of tile types and their costs
// costs are for pathfinding, nodepaths are to get the right
// tile when spawning them
enum Tile {
    GrassTile(10, false, "GrassTile1"),
    RockTile(10, false, "RockTile"),
    GrassProp(10, false, "GrassProp"),
    RockProp(10, false, "RockProp"),
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

    private final double cellWidth = 2.0;
    private final int mapWidth = 80;
    private final int mapHeight = 80;

    private GridCell[][] grid;

    private FastNoiseLite baseNoise;

    private int noiseSeed = (int) (Math.random() * 1000);
    private float noiseFreq = 0.08f;

    private Queue<Node3D> spawnedTiles;

    @RegisterFunction
    @Override
    public void _ready() {
        grid = new GridCell[mapWidth][mapHeight];
        spawnedTiles = new LinkedList<>();
        populateGrid();
    }

    @RegisterFunction
    @Override
    public void _process(double delta) {
        if (!spawnedTiles.isEmpty()) {
            getParent().addChild(spawnedTiles.poll());
        }
    }

    // fill grid[][] with the proc gen tiles
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
                if (height < 0.15) cell.setTile(Tile.Empty);
                cell.setHeight(height * 10);
                spawnTile(cell);
                grid[x][z] = cell;
            }
        }
    }

    // samples the terrain noise texture
    private double sampleNoise(int x, int z) {
        double val = baseNoise.getNoise2d(x, z);
        return val;
    }

    // gets world coords from the x and z indices
    private Coordinate getCoord(int xIndex, int zIndex) {
        // position of each axis, scaled to -w to w and -h to h
        double px = xIndex * cellWidth - (mapWidth * cellWidth) / 2;
        double pz = zIndex * cellWidth - (mapHeight * cellWidth) / 2;
        Coordinate newCoord = new Coordinate(px, pz, xIndex, zIndex);
        return newCoord;
    }

    // samples the biome noise texture to get the tile type
    // todo!
    private Tile getTileType(int x, int z) {
        return Tile.GrassTile;
    }

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
        float scale = 1.0f - (float) (Math.random() * 0.2);
        tileInstance.setPosition(pos);
        tileInstance.setRotation(
            new Vector3(
                0,
                (Math.PI / 2) * (Math.random() * 4) + Math.random() * 0.1,
                0
            )
        );
        tileInstance.setScale(new Vector3(scale, scale, scale));

        gd.print(pos);

        spawnedTiles.add(tileInstance);
    }

    // pathfinding (scream emoji)
    // its 12 am but i really want to finish this
    @RegisterFunction
    public ArrayList<Coordinate> navigate(Vector2 startPos, Vector2 endPos) {
        long startTime = System.nanoTime();
        // get the grid cells that correspond with the world positions
        GridCell start = coordToGrid(startPos);
        GridCell end = coordToGrid(endPos);

        gd.print("began pathfinding\n" + start + " to " + end);

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

        gd.print(
            cameFrom.size() +
            " nodes in " +
            (duration / 1_000_000.0) +
            " milliseconds"
        );

        return path;
    }

    // this is prolly the cause of all my problems
    private double getHeuristic(GridCell a, GridCell b) {
        int dx = Math.abs(
            a.getCoords().getXIndex() - b.getCoords().getXIndex()
        );
        int dz = Math.abs(
            a.getCoords().getZIndex() - b.getCoords().getZIndex()
        );
        return dx + dz;
    }

    // gets the cost
    // this is what that funny enum is for
    private double getCost(GridCell next) {
        return next.getTile().cost;
    }

    // gets gridcell from world coord (i dont want to write this)
    // i sure hope this works lmao
    private GridCell coordToGrid(Vector2 coords) {
        int xi = (int) ((coords.getX() + (mapWidth * cellWidth) / 2) /
            cellWidth);
        int zi = (int) ((coords.getY() + (mapHeight * cellWidth) / 2) /
            cellWidth);
        return grid[xi][zi];
    }

    // gets neighboring walkable cells
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

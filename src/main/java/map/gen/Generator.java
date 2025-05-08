package map.gen;

import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.AStarGrid2D.Heuristic;
import godot.api.FastNoiseLite;
import godot.api.FastNoiseLite.NoiseType;
import godot.api.Node3D;
import godot.core.Vector2;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

// enum to keep track of tile types and their costs
// costs are for pathfinding, nodepaths are to get the right
// tile when spawning them
enum Tile {
    GrassTile(10, false, "GrassTile"),
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

    private final double cellWidth = 2.0;
    private final int mapWidth = 60;
    private final int mapHeight = 60;

    private GridCell[][] grid;

    private FastNoiseLite baseNoise;

    private int noiseSeed = (int) (Math.random() * 1000);
    private float noiseFreq = 0.5f;

    @RegisterFunction
    @Override
    public void _ready() {
        populateGrid();
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
                if (height < 0) cell.setTile(Tile.Empty);
                spawnTile(cell);
                grid[x][z] = cell;
            }
        }
    }

    // samples the terrain noise texture
    private double sampleNoise(int x, int z) {
        x = x / mapWidth;
        z = z / mapHeight;
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
        // todo soon
    }

    // pathfinding (scream emoji)
    // its 12 am but i really want to finish this
    public ArrayList<Coordinate> navigate(Vector2 startPos, Vector2 endPos) {
        // get the grid cells that correspond with the world positions
        GridCell start = coordToGrid(startPos);
        GridCell end = coordToGrid(endPos);

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
        return path;
    }

    private double getHeuristic(GridCell a, GridCell b) {
        return (
            Math.abs(a.getCoords().getX() - b.getCoords().getX()) +
            Math.abs(a.getCoords().getZ() - b.getCoords().getZ())
        );
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

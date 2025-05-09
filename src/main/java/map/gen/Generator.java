package map.gen;

import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.CSGBox3D;
import godot.api.FastNoiseLite;
import godot.api.FastNoiseLite.NoiseType;
import godot.api.Node3D;
import godot.api.StandardMaterial3D;
import godot.api.Timer;
import godot.core.Callable;
import godot.core.Color;
import godot.core.StringNames;
import godot.core.Vector2;
import godot.core.Vector3;
import godot.global.GD;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

// enum to keep track of tile types and their costs
// costs are for pathfinding, nodepaths are to get the right
// tile when spawning them
enum Tile {
    GrassTile(10, true, "GrassTile"),
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

/**
 * Visualization step class for storing the visualization sequence
 */
class VisStep {

    public enum StepType {
        RESET,
        FRONTIER,
        VISITED,
        PATH,
        START_END,
    }

    public StepType type;
    public GridCell cell;
    public Color color;

    public VisStep(StepType type, GridCell cell, Color color) {
        this.type = type;
        this.cell = cell;
        this.color = color;
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
    private final int mapWidth = 60;
    private final int mapHeight = 60;

    private GridCell[][] grid;
    private HashMap<GridCell, CSGBox3D> visualNodes = new HashMap<>();

    private FastNoiseLite baseNoise;

    private int noiseSeed = (int) (Math.random() * 1000);
    private float noiseFreq = 0.5f;

    // Colors for visualization
    private final Color COLOR_UNVISITED = new Color(0.8f, 0.8f, 0.8f, 1.0f); // Light gray
    private final Color COLOR_FRONTIER = new Color(0.0f, 0.7f, 1.0f, 1.0f); // Cyan
    private final Color COLOR_VISITED = new Color(0.5f, 0.5f, 0.5f, 1.0f); // Gray
    private final Color COLOR_PATH = new Color(1.0f, 1.0f, 0.0f, 1.0f); // Yellow
    private final Color COLOR_START = new Color(0.0f, 1.0f, 0.0f, 1.0f); // Green
    private final Color COLOR_END = new Color(1.0f, 0.0f, 0.0f, 1.0f); // Red
    private final Color COLOR_OBSTACLE = new Color(0.3f, 0.3f, 0.3f, 1.0f); // Dark gray

    // Visualization queue and timer
    private Queue<VisStep> visualizationQueue = new LinkedList<>();
    private Timer visualizationTimer;
    private float visualizationStepDelay = 0.02f; // 20ms between steps (50 steps per second)
    private boolean isVisualizing = false;

    // Path result storage
    private ArrayList<Coordinate> pathResult = null;

    @RegisterFunction
    @Override
    public void _ready() {
        grid = new GridCell[mapWidth][mapHeight];
        populateGrid();

        // Setup the visualization timer
        visualizationTimer = new Timer();
        visualizationTimer.setOneShot(false);
        visualizationTimer.setWaitTime(visualizationStepDelay);

        // Connect the signal with the recommended way from the documentation
        visualizationTimer.connect(
            "timeout",
            Callable.create(
                this,
                StringNames.toGodotName("_on_visualization_timer_timeout")
            )
        );

        addChild(visualizationTimer);

        createVisualGrid();

        //testing purposes - create some obstacles
        grid[32][29].setTile(Tile.GrassProp);
        grid[32][28].setTile(Tile.GrassProp);
        grid[32][27].setTile(Tile.GrassProp);
        grid[32][26].setTile(Tile.GrassProp);
        grid[32][25].setTile(Tile.GrassProp);
        grid[32][30].setTile(Tile.GrassProp);
        grid[32][31].setTile(Tile.GrassProp);
        grid[33][31].setTile(Tile.GrassProp);
        grid[34][31].setTile(Tile.GrassProp);
        grid[35][31].setTile(Tile.GrassProp);

        // Update visualization for obstacles
        for (int x = 0; x < mapWidth; x++) {
            for (int z = 0; z < mapHeight; z++) {
                GridCell cell = grid[x][z];
                if (cell.getTile() == Tile.GrassProp) {
                    // Make obstacle boxes taller
                    CSGBox3D box = visualNodes.get(cell);
                    updateNodeColor(cell, COLOR_OBSTACLE);
                    box.setSize(
                        new Vector3((float) cellWidth, 1.0f, (float) cellWidth)
                    );
                    box.setUseCollision(true);
                }
            }
        }
    }

    @RegisterFunction
    public void _on_visualization_timer_timeout() {
        if (!visualizationQueue.isEmpty()) {
            VisStep step = visualizationQueue.poll();

            // Process visualization step
            if (step.type == VisStep.StepType.RESET) {
                resetVisualizationImmediate();
            } else {
                updateNodeColor(step.cell, step.color);
            }
        } else {
            // Stop the timer when queue is empty
            visualizationTimer.stop();
            isVisualizing = false;

            // Signal that pathfinding is complete
            gd.print("Visualization complete!");
        }
    }

    // Create a visual representation of the grid
    private void createVisualGrid() {
        for (int x = 0; x < mapWidth; x++) {
            for (int z = 0; z < mapHeight; z++) {
                GridCell cell = grid[x][z];
                CSGBox3D box = new CSGBox3D();

                // Make the visualization boxes a bit shorter than obstacles
                box.setSize(
                    new Vector3(
                        (float) cellWidth * 0.9f,
                        0.1f,
                        (float) cellWidth * 0.9f
                    )
                );
                box.setPosition(
                    new Vector3(
                        (float) cell.getCoords().getX(),
                        0.05f, // Slightly above ground
                        (float) cell.getCoords().getZ()
                    )
                );

                // Set default color
                updateNodeColor(box, COLOR_UNVISITED);

                addChild(box);
                visualNodes.put(cell, box);
            }
        }
    }

    // Update material color for a visualization node
    private void updateNodeColor(CSGBox3D box, Color color) {
        StandardMaterial3D material = new StandardMaterial3D();
        material.setAlbedo(color);
        box.setMaterial(material);
    }

    // Update node color by cell
    private void updateNodeColor(GridCell cell, Color color) {
        CSGBox3D box = visualNodes.get(cell);
        if (box != null) {
            updateNodeColor(box, color);
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
                cell.setTile(Tile.Empty);
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
        //todo
    }

    // pathfinding (scream emoji)
    // its 12 am but i really want to finish this
    @RegisterFunction
    public ArrayList<Coordinate> navigate(Vector2 startPos, Vector2 endPos) {
        // If we're already visualizing, don't start a new pathfinding operation
        if (isVisualizing) {
            gd.print("Already visualizing a path. Please wait.");
            return new ArrayList<>();
        }

        // Reset visualization and prepare for a new path
        visualizationQueue.clear();
        visualizationQueue.add(new VisStep(VisStep.StepType.RESET, null, null));

        // get the grid cells that correspond with the world positions
        GridCell start = coordToGrid(startPos);
        GridCell end = coordToGrid(endPos);

        // Mark start and end points (add to visualization queue)
        visualizationQueue.add(
            new VisStep(VisStep.StepType.START_END, start, COLOR_START)
        );
        visualizationQueue.add(
            new VisStep(VisStep.StepType.START_END, end, COLOR_END)
        );

        gd.print("began pathfinding\n" + start + " to " + end);

        // a* algorithm 😱
        PriorityQueue<Pair<GridCell, Double>> frontier = new PriorityQueue<>(
            Comparator.comparingDouble(Pair::getSecond)
        );
        frontier.add(new Pair<GridCell, Double>(start, 0d));

        // Keep track of cells in frontier for visualization
        HashSet<GridCell> frontierSet = new HashSet<>();
        frontierSet.add(start);

        HashMap<GridCell, GridCell> cameFrom = new HashMap<>();
        cameFrom.put(start, null);
        HashMap<GridCell, Double> gCost = new HashMap<>();
        gCost.put(start, 0d);

        while (!frontier.isEmpty()) {
            GridCell currentCell = frontier.poll().getFirst();
            frontierSet.remove(currentCell);

            // Don't color start/end nodes
            if (currentCell != start && currentCell != end) {
                visualizationQueue.add(
                    new VisStep(
                        VisStep.StepType.VISITED,
                        currentCell,
                        COLOR_VISITED
                    )
                );
            }

            gd.print("Current cell: " + currentCell);

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

                    // Visualize frontier
                    if (next != end && !frontierSet.contains(next)) {
                        frontierSet.add(next);
                        visualizationQueue.add(
                            new VisStep(
                                VisStep.StepType.FRONTIER,
                                next,
                                COLOR_FRONTIER
                            )
                        );
                    }
                }
            }
        }

        // go backwards from end cell to get path
        ArrayList<Coordinate> path = new ArrayList<>();
        GridCell current = end;

        if (cameFrom.get(end) != null) { // Only if path was found
            while (current != start) {
                path.add(current.getCoords());
                if (current != end) { // Don't color the end node
                    visualizationQueue.add(
                        new VisStep(VisStep.StepType.PATH, current, COLOR_PATH)
                    );
                }
                current = cameFrom.get(current);
            }

            path.add(start.getCoords());
            Collections.reverse(path);
        } else {
            gd.print("No path found!");
        }

        gd.print("Path calculation finished, starting visualization");

        // Store the path for access later
        pathResult = path;

        // Start the visualization timer
        isVisualizing = true;
        visualizationTimer.start();

        return path;
    }

    // Reset visualization colors immediately (not queued)
    private void resetVisualizationImmediate() {
        for (int x = 0; x < mapWidth; x++) {
            for (int z = 0; z < mapHeight; z++) {
                GridCell cell = grid[x][z];
                if (cell.getTile() == Tile.GrassProp) {
                    updateNodeColor(cell, COLOR_OBSTACLE);
                } else {
                    updateNodeColor(cell, COLOR_UNVISITED);
                }
            }
        }
    }

    private double getHeuristic(GridCell a, GridCell b) {
        double dx = a.getCoords().getX() - b.getCoords().getX();
        double dz = a.getCoords().getZ() - b.getCoords().getZ();
        return Math.sqrt(dx * dx + dz * dz);
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
        // Clamp to grid bounds
        xi = Math.max(0, Math.min(mapWidth - 1, xi));
        zi = Math.max(0, Math.min(mapHeight - 1, zi));
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

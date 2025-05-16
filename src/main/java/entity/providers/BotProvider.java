package entity.providers;

import entity.InputProvider;
import entity.InputState;
import entity.Ship;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.Input;
import godot.core.Vector2;
import godot.core.Vector3;
import godot.global.GD;
import java.util.ArrayList;
import java.util.List;
import map.gen.Coordinate;
import map.gen.Generator;

@RegisterClass
public class BotProvider extends InputProvider {

    private static final GD gd = GD.INSTANCE;

    private final double VELOCITY_STEP = 3.0;
    private final double ROTATION_STEP = 4.0;
    private double rotation;
    private double velocity;
    private int selectedAction;
    private boolean emitAction;
    private InputState currentState;

    private int frameCounter;

    private ArrayList<Coordinate> path;
    private Coordinate currentNode;

    private Generator gen;

    /** _ready
     * runs upon being instantiated in the game world
     * acts as a constructor
     */
    @RegisterFunction
    @Override
    public void _ready() {
        gd.print("Loaded bot provider");
        currentState = new InputState();
        rotation = 0;
        velocity = 0;
        selectedAction = 0;
        emitAction = false;

        frameCounter = 0;
    }

    @RegisterFunction
    @Override
    public void _process(double delta) {
        if (frameCounter % 60 == 0) updatePath();
        frameCounter++;
    }

    private void updatePath() {
        ArrayList<Coordinate> tempPath = gen.navigate(
            getGlobalPosition(),
            getTargetPosition()
        );
    }

    private Vector3 getTargetPosition() {
        return new Vector3(0, 0, 0);
    }

    private void adjustCurrentNode() {}

    private Vector3 getAverageNode(int nodes) {
        return new Vector3();
    }
}

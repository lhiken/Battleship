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
    //    private int selectedAction;
//    private boolean emitAction;
    private InputState currentState;
    private int monkey;

    private boolean enemyWithinRadius = false;
    private Vector3 targetPos;
    private Vector3 startPos;

    private ArrayList<Coordinate> path;

    private int frameCounter;

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
        targetPos = new Vector3(0, 0, 60);
        startPos = this.getGlobalPosition();
        path = gen.navigate(startPos, targetPos);
//        selectedAction = 0;
//        emitAction = false;

        frameCounter = 0;
    }

    @RegisterFunction
    @Override
    public void _process(double delta) {

        if (!enemyWithinRadius) {

            wander();

        }

        moveToPoint();

        updateState();

    }

    public void wander() {

        if (this.getGlobalPosition().isEqualApprox(targetPos)) {
            do {
                double randomAngle = Math.random() * 360;
                double distance = Math.random() * 65;
                targetPos = new Vector3(Math.cos(Math.toRadians(randomAngle)) * distance, 0, Math.sin(Math.toRadians(randomAngle)) * distance);
                gd.print("Target position:" + targetPos);
            } while (!targetPos.equals(new Vector3(0, 0, 0))); // random coordinate generated is on the island

            startPos = this.getGlobalPosition();
            gd.print("Start Position:" + startPos);
            path = gen.navigate(startPos, targetPos);

        }

    }

    public void chase() {

    }

    public void moveToPoint() {

        Coordinate temp = path.get(0);
        Vector3 target = temp.toVec3();

        Vector3 difference = target.minus(this.getGlobalPosition());

        if (difference.isZeroApprox()) {
            path.remove(0);
        }


        rotation = this.getGlobalPosition().angleTo(target);  // this is definitely not right
        velocity = 1; // this might be right

    }

    private void updateState() {
        currentState.velocity = velocity;
        currentState.rotation = rotation;
    }

    @RegisterFunction
    @Override
    public InputState getState() {
        return currentState;
    }


    private void adjustCurrentNode() {}

    private Vector3 getAverageNode(int nodes) {
        return new Vector3();
    }

}

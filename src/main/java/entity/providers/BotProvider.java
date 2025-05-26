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
    private double power;
    private InputState currentState;

    private double turretPitch;
    private double turretYaw;

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

        //testing aim
        Ship target = (Ship) getParent().getParent().getNode("1");
        Ship thisShip = (Ship) getParent();

        setAimDirection(
            target
                .getGlobalPosition()
                .plus(
                    new Vector3(
                        0,
                        target
                                .getGlobalPosition()
                                .distanceTo(thisShip.getGlobalPosition()) /
                            30.0 -
                        1,
                        0
                    )
                ),
            thisShip.getGlobalPosition(),
            target.velocityProperty(),
            thisShip.velocityProperty(),
            25.0
        );

        emitAction = true;
        selectedAction = 1;

        gen.visualizePath(path);

        updateState();
    }

    public void wander() {
        if (this.getGlobalPosition().isEqualApprox(targetPos)) {
            do {
                double randomAngle = Math.random() * 360;
                double distance = Math.random() * 65;
                targetPos = new Vector3(
                    Math.cos(Math.toRadians(randomAngle)) * distance,
                    0,
                    Math.sin(Math.toRadians(randomAngle)) * distance
                );
                gd.print("Target position:" + targetPos);
            } while (!targetPos.equals(new Vector3(0, 0, 0))); // random coordinate generated is on the island

            startPos = this.getGlobalPosition();
            gd.print("Start Position:" + startPos);
            path = gen.navigate(startPos, targetPos);
        }
    }

    public void chase() {}

    public void moveToPoint() {
        Coordinate temp = path.get(0);
        Vector3 target = temp.toVec3();

        Vector3 difference = target.minus(this.getGlobalPosition());

        if (difference.isZeroApprox()) {
            path.remove(0);
        }

        rotation = this.getGlobalPosition().angleTo(target); // this is definitely not right
        velocity = 1; // this might be right
    }

    private void updateState() {
        currentState.velocity = velocity;
        currentState.rotation = rotation;
        currentState.emitAction = emitAction ? selectedAction : -1;
        currentState.power = Math.min(14, power * 3);
        currentState.turretYaw = turretYaw;
        currentState.turretPitch = turretPitch;
    }

    @RegisterFunction
    @Override
    public InputState getState() {
        return currentState;
    }

    @RegisterFunction
    public void setGenerator(Generator gen) {
        this.gen = gen;
    }

    private void adjustCurrentNode() {}

    private Vector3 getAverageNode(int nodes) {
        return new Vector3();
    }

    private void setAimDirection(
        Vector3 P_t,
        Vector3 P_s,
        Vector3 V_t,
        Vector3 V_s,
        double S_p
    ) {
        double t = 2.0;

        // newtons method with like 5 iterations
        for (int i = 0; i < 5; i++) {
            double f = f_t(P_t, P_s, V_t, V_s, S_p, t);
            double f_p = f_pt(P_t, P_s, V_t, V_s, S_p, t);

            if (Math.abs(f_p) < 1e-8) break;

            t = t - f / f_p;
        }

        // get direction based on solved t
        Vector3 direction = getProjectileVelocity(
            P_t,
            P_s,
            V_t,
            V_s,
            t
        ).normalized();

        // convert direction into yaw and pitch input
        turretPitch = Math.asin(direction.getY() / direction.length());
        turretYaw = Math.atan2(direction.getX(), direction.getZ());
    }

    private Vector3 getProjectileVelocity(
        Vector3 P_t,
        Vector3 P_s,
        Vector3 V_t,
        Vector3 V_s,
        double t
    ) {
        Vector3 g = new Vector3(0, -9.8, 0);
        return P_t.plus(V_t.times(t))
            .minus(P_s.plus(V_s.times(t)))
            .minus(g.times(t * t * 0.5))
            .div(t);
    }

    // f(t) vector
    private double f_t(
        Vector3 P_t,
        Vector3 P_s,
        Vector3 V_t,
        Vector3 V_s,
        double S_p,
        double t
    ) {
        return (u_t(P_t, P_s, V_t, V_s, S_p, t).length() - S_p * t);
    }

    // f'(t) vector
    private double f_pt(
        Vector3 P_t,
        Vector3 P_s,
        Vector3 V_t,
        Vector3 V_s,
        double S_p,
        double t
    ) {
        Vector3 u_t = u_t(P_t, P_s, V_t, V_s, S_p, t);
        Vector3 u_pt = u_pt(V_t, V_s, t);
        Vector3 g = new Vector3(0, -9.8, 0);
        return u_t.dot(u_pt) / u_t.length() - S_p;
    }

    // u(t) vector
    private Vector3 u_t(
        Vector3 P_t,
        Vector3 P_s,
        Vector3 V_t,
        Vector3 V_s,
        double S_p,
        double t
    ) {
        Vector3 R = P_t.minus(P_s); // relative distance
        Vector3 g = new Vector3(0, -9.8, 0);
        return (R.plus(V_t.minus(V_s).times(t)).minus(g.times(t * t * 0.5)));
    }

    // u'(t) vector
    private Vector3 u_pt(Vector3 V_t, Vector3 V_s, double t) {
        Vector3 g = new Vector3(0, -9.8, 0);
        return (V_t.minus(V_s)).minus(g.times(t));
    }
}

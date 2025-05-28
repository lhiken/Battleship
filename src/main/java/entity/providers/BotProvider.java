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
    private double time;
    private double delta2;

    private ArrayList<Coordinate> path;

    private int frameCounter;

    private Coordinate currentNode;

    private Generator gen;

    private double actualTargetRotation = 0.0;

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
        targetPos = getRandomPosition();
        startPos = this.getGlobalPosition();
        path = gen.navigate(startPos, targetPos);
        smoothOutPath(path);
        selectedAction = 0;
        emitAction = false;
        frameCounter = 0;
    }

    @RegisterFunction
    @Override
    public void _process(double delta) {
        delta2 = delta;
        time += delta;

        if (!enemyWithinRadius) {
            wander();
        }

        moveToPoint();

        //testing aim
        Ship target = (Ship) getParent().getParent().getNode("1");
        Ship thisShip = (Ship) getParent();

        if (target == null) return;

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

        if (target != null) {
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
        }

        emitAction = true;
        selectedAction = 1;

        gen.visualizePath(path);

        updateState();
    }

    public void wander() {
//        if (path.isEmpty()) {
//            do {
//                path = gen.navigate(
//                    this.getGlobalPosition(),
//                    getRandomPosition()
//                );
//                smoothOutPath(path);
//            } while (path != null); // random coordinate generated is on the island
//
//            startPos = this.getGlobalPosition();
//        }
//            startPos = this.getGlobalPosition();

        gd.print("Path size" + path.size());
        if (path.size() < 10) {
            gd.print("Finding new path");
            ArrayList<Coordinate> newPath = new ArrayList<Coordinate>();
            Vector3 previousTargetPos = targetPos;

            newPath = gen.navigate(previousTargetPos, getRandomPosition());

//            boolean validPath = false;
//            int iterations = 0;
//
//            while (!validPath) {
//
//                newPath = gen.navigate(previousTargetPos, getRandomPosition());
//
//                Vector3 point1 = path.get(path.size() - 2).toVec3();
//                Vector3 point2 = path.get(path.size() - 1).toVec3();
//                Vector3 point3 = newPath.get(0).toVec3();
//
//                if (!(point2.minus(point1).angleTo(point3.minus(point2)) < (Math.PI) / 2) || iterations > 20) {
//
//                    validPath = true;
//
//                }
//
//                iterations++;
//
//            }

            path.addAll(newPath);
            smoothOutPath(path);

            targetPos = path.get(path.size()-1).toVec3();

        }
    }

    public void smoothOutPath(ArrayList<Coordinate> path) {
        for (int j = 0; j < 5; j++) {
            path.add(0, path.get(0));
            for (int i = 0; i < path.size() - 1; i++) {
                double averageX =
                    (path.get(i).getX() + path.get(i + 1).getX()) / 2;
                double averageZ =
                    (path.get(i).getZ() + path.get(i + 1).getZ()) / 2;
                path.set(i, new Coordinate(averageX, averageZ, i, i));
            }
        }
    }

    public void chase() {}

    private Vector3 getRandomPosition() {
        Vector3 position = null;

        while (position == null) {
            double direction = Math.random() * 2 * Math.PI;
            double distance = Math.random() * 60;

            Vector3 pos = new Vector3(
                Math.cos(direction),
                0,
                Math.sin(direction)
            );

            pos = pos.times(distance);

            if (gen.checkWalkable(pos)) position = pos;
        }

        return position;
    }

    public void moveToPoint() {
//        if (path.size() < 3) {
//            path.addAll(gen.navigate(getGlobalPosition(), getRandomPosition()));
//            smoothOutPath(path);
//        }

        Vector3 curr = path.get(0).toVec3();
        Vector3 next = path.size() >= 2 ? path.get(1).toVec3() : curr;
        double shipYaw = ((Ship) getParent()).getGlobalRotation().getY();
        Vector3 shipToCurr = getGlobalPosition().minus(curr);
        Vector3 currToNext = curr.minus(next);
        Vector3 shipDirection = new Vector3(
            Math.sin(shipYaw),
            0,
            Math.cos(shipYaw)
        );
        Vector3 currTargetDirection =
            this.getGlobalPosition().directionTo(curr);

        // shows if the ship is past the current target
        double pastCurr = shipToCurr.dot(currToNext);
        gd.print(pastCurr);

        if (curr.distanceTo(getGlobalPosition()) < 0.3 || pastCurr < 0.5) {
            path.remove(0);
        }

        double expectedRotation = Math.atan2(
            currTargetDirection.getX(),
            currTargetDirection.getZ()
        );



        double rotationInput = normalizeAngle(
            expectedRotation - getGlobalRotation().getY()
        );
        rotation += gd.clamp(rotationInput, gd.degToRad(-1), gd.degToRad(1));

        velocity = 1.0; // this might be right
    }

    private double normalizeAngle(double angle) {
        angle = angle % (2 * Math.PI);
        if (angle > Math.PI) angle -= 2 * Math.PI;
        if (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
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
        double yaw = Math.atan2(direction.getX(), direction.getZ());
        turretYaw = yaw;
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

    // f(t) scalar
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

    // f'(t) scalar
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

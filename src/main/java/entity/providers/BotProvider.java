package entity.providers;

import entity.InputProvider;
import entity.InputState;
import entity.Ship;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.Node;
import godot.api.Node3D;
import godot.core.VariantArray;
import godot.core.Vector3;
import godot.global.GD;
import java.util.ArrayList;
import map.gen.Coordinate;
import map.gen.Generator;
import multiplayer.MultiplayerManager;

/**
 * BotProvider that extends InputProvider
 * A version of InputProvider that handles inputs for a bot ship (usually in the form of environmental factors)
 * <p>
 * In its process method, BotProvider uses environmental factors to find it's current state
 * Either WANDER, CHASE, or RUN
 * and then generates a movement path to follow depending on it's state
 * Will also calculate the predicted movement of a ship and shoot when possible (and depending on it's state)
 * <p>
 * Also contains getter and setter methods
 * As well as helper methods such as Newton's Method calculator and more
 */
@RegisterClass
public class BotProvider extends InputProvider {

    private static final GD gd = GD.INSTANCE;

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
    private Generator gen;
    private Ship targettedShip;
    private double time;

    /**
     * Overrides Godot's internal built-in _ready function
     * Runs upon being instantiated in the game world
     * And acts as a constructor
     * Automatically creating a path for the bot to follow and setting up multiplayer status
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
        emitAction = false;
        selectedAction = 1;
        setMultiplayerAuthority(1);
    }

    /**
     * Overrides Godot's internal built-in ready function
     * Calls its various helper methods in order to determine the path it needs to take during each frame
     * As well as where it should aim in order to hit other ships
     * @param delta the time elapsed between each call to _process
     */
    @RegisterFunction
    @Override
    public void _process(double delta) {
        time += delta;

        MultiplayerManager manager = MultiplayerManager.Instance;
        if (!manager.isServer()) return;

        if (!enemyWithinRadius()) {
            wander();
        } else {
            chase();
        }

        moveToPoint();
        handleTargetting();

        gen.visualizePath(path);

        updateState();
    }

    /**
     * The WANDER state
     * <p>
     * If it's current path size is less than ten, generate a new path
     * Append the new path to the previous path (so it always has somewhere to move to)
     */
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

            targetPos = path.get(path.size() - 1).toVec3();
        }
    }

    /**
     * Helper method for the CHASE state
     * <p>
     * Gets an ArrayList of all ships in the match and finds the closest ship
     * If the distance from its own ship to the closest ship is less than a certain amount
     * Then enter CHASE state
     * <p>
     * If previous path is nearby to ship, does not make a new path to avoid
     * Messy rotation inputs
     * @return a boolean determining if an enemy ship is within tracking radius
     */
    public boolean enemyWithinRadius() {
        Ship ownShip = (Ship) getParent();

        Node3D ships = (Node3D) getParent()
            .getParent()
            .getParent()
            .getNode("Ships");
        VariantArray<Node> temp = ships.getChildren();
        ArrayList<Ship> shipInfo = new ArrayList<Ship>();

        for (Node ship : temp) {
            if (!(ship.equals(ownShip))) {
                shipInfo.add((Ship) ship);
            }
        }

        Vector3 closestShipLoc = new Vector3(1000, 0, 1000);
        Ship trackedShip = new Ship();
        for (Ship ship : shipInfo) {
            if (
                !ship.isSinking() &&
                (this.getGlobalPosition()
                        .minus(ship.getGlobalPosition())).length() <
                (this.getGlobalPosition().minus(closestShipLoc)).length()
            ) {
                trackedShip = ship;
                closestShipLoc = ship.getGlobalPosition();
            }
        }

        if ((this.getGlobalPosition().minus(closestShipLoc)).length() < 30) {
            if (closestShipLoc.minus(targetPos).length() > 10) {
                path = gen.navigate(
                    this.getGlobalPosition(),
                    getRandomPosition(
                        this.getGlobalPosition(),
                        trackedShip.getGlobalPosition(),
                        5
                    )
                );
                smoothOutPath(path);
                targetPos = path.get(path.size() - 1).toVec3();
                startPos = path.get(0).toVec3();
            }
            return true;
        } else {
            return false;
        }
    }

    private void handleTargetting() {
        Ship thisShip = (Ship) getParent();

        if (targettedShip != null) {
            setAimDirection(
                targettedShip
                    .getGlobalPosition()
                    .plus(
                        new Vector3(
                            0,
                            targettedShip
                                    .getGlobalPosition()
                                    .distanceTo(
                                        targettedShip.getGlobalPosition()
                                    ) /
                                30.0 -
                            1,
                            0
                        )
                    ),
                thisShip.getGlobalPosition(),
                targettedShip.velocityProperty(),
                thisShip.velocityProperty(),
                25.0
            );
            turretPitch = gd.clamp(
                turretPitch,
                gd.degToRad(-15),
                gd.degToRad(25)
            );

            if (canShoot()) {
                emitAction = true;
            } else {
                emitAction = false;
                turretYaw = getGlobalRotation().getY();
                turretPitch = 0;
            }
        }
    }

    /**
     * Takes a path and smooths it out into more of a curve
     * In order to prevent sharp angles and turns for the ship to move in
     * @param path the path to smooth out
     */
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
        targetPos = path.get(path.size() - 1).toVec3();
    }

    /**
     * The CHASE state
     *
     * Gets an ArrayList of all ships
     * And generates a path that goes to a place near the closest enemy ship (but not directly to it)
     */
    public void chase() {
        Ship ownShip = (Ship) getParent();

        Node3D ships = (Node3D) getParent()
            .getParent()
            .getParent()
            .getNode("Ships");
        VariantArray<Node> temp = ships.getChildren();
        ArrayList<Ship> shipInfo = new ArrayList<Ship>();

        for (Node ship : temp) {
            if (!(ship.equals(ownShip))) {
                shipInfo.add((Ship) ship);
            }
        }

        Vector3 closestShipLoc = new Vector3(1000, 0, 1000);
        Ship trackedShip = new Ship();

        for (Ship ship : shipInfo) {
            if (
                (this.getGlobalPosition()
                            .minus(ship.getGlobalPosition())).length() <
                    (this.getGlobalPosition().minus(closestShipLoc)).length() &&
                ship.getGlobalPosition() != ownShip.getGlobalPosition()
            ) {
                trackedShip = ship;
                closestShipLoc = ship.getGlobalPosition();
            }
        }

        targettedShip = trackedShip;
        if ((this.getGlobalPosition().minus(closestShipLoc)).length() < 30) {
            if (
                closestShipLoc.minus(targetPos).length() > 10 || path.size() < 1
            ) {
                path = gen.navigate(
                    this.getGlobalPosition(),
                    getRandomPosition(
                        this.getGlobalPosition(),
                        trackedShip.getGlobalPosition(),
                        5
                    )
                );
                smoothOutPath(path);
                targetPos = path.get(path.size() - 1).toVec3();
                startPos = path.get(0).toVec3();
            }
        }
    }

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

    private Vector3 getRandomPosition(
        Vector3 myShip,
        Vector3 trackedShip,
        double radius
    ) {
        Vector3 position = null;

        while (position == null) {
            double direction = Math.random() * 2 * Math.PI;
            double distance = Math.random() * 2 + radius - 1;

            Vector3 pos = new Vector3(
                Math.cos(direction),
                0,
                Math.sin(direction)
            );

            pos = trackedShip.plus(pos.times(distance));

            if (gen.checkWalkable(pos)) position = pos;
        }

        return position;
    }

    /**
     * Given a path that the ship has:
     * a) Calculates the rotation and velocity needed in order to travel to the next point
     * b) Skips points if suitable (based on dot products and angle differences)
     */
    public void moveToPoint() {
        if (path.size() == 0) return;

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
        Vector3 nextTargetDirection =
            this.getGlobalPosition().directionTo(next);

        // shows if the ship is past the current target
        double pastCurr = shipToCurr.dot(currToNext);

        if (curr.distanceTo(getGlobalPosition()) < 0.3 || pastCurr < 0.5) {
            path.remove(0);
        }

        Vector3 finalDirection = currTargetDirection.dot(shipDirection) <
            nextTargetDirection.dot(shipDirection)
            ? nextTargetDirection
            : currTargetDirection;

        double expectedRotation = Math.atan2(
            finalDirection.getX(),
            finalDirection.getZ()
        );

        // double rotationInput = normalizeAngle(
        //     expectedRotation - getGlobalRotation().getY()
        // );
        // rotation += gd.clamp(rotationInput, gd.degToRad(-2), gd.degToRad(2));
        double lerpFactor = gd.lerp(
            0.6,
            1.0,
            gd.smoothstep(
                0.8,
                1.0,
                gd.clamp(
                    (currTargetDirection
                            .normalized()
                            .dot(shipDirection.normalized()) /
                        2) +
                    0.5,
                    0,
                    1
                )
            )
        );
        rotation = gd.lerpAngle(rotation, expectedRotation, lerpFactor * 0.05);

        velocity = 0.9;
    }

    private double normalizeAngle(double angle) {
        angle = angle % (2 * Math.PI);
        if (angle > Math.PI) angle -= 2 * Math.PI;
        if (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }

    private boolean canShoot() {
        double rot = getGlobalRotation().getY() - Math.PI;
        double diff = normalizeAngle(rot - turretYaw);
        return (
            targettedShip.getGlobalPosition().distanceTo(getGlobalPosition()) <
                20 &&
            Math.abs(gd.radToDeg(diff)) > 30 &&
            turretPitch > gd.degToRad(-10)
        );
    }

    private void updateState() {
        currentState.velocity = velocity;
        currentState.rotation = rotation;
        currentState.emitAction = emitAction ? selectedAction : -1;
        currentState.power = Math.min(14, power * 3);
        currentState.turretYaw = turretYaw;
        currentState.turretPitch = turretPitch;
    }

    /**
     * Getter method for the InputState
     * @return The InputState
     */
    @RegisterFunction
    @Override
    public InputState getState() {
        return currentState;
    }

    /**
     * Setter method for the generator
     * @param gen The generator to be set
     */
    @RegisterFunction
    public void setGenerator(Generator gen) {
        this.gen = gen;
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

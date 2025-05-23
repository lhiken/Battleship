package entity.weapon;

import entity.Ship;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.MeshInstance3D;
import godot.api.Node3D;
import godot.core.EulerOrder;
import godot.core.Vector3;
import godot.global.GD;

@RegisterClass
public class Turret extends MeshInstance3D {

    private GD gd = GD.INSTANCE;
    private Ship ship;
    private Node3D cannon;

    private final double CAMERA_PITCH_MAX = gd.degToRad(5);
    private final double CAMERA_PITCH_MIN = gd.degToRad(50);

    private double yaw;
    private double pitch;

    @RegisterFunction
    @Override
    public void _ready() {
        ship = (Ship) getParent();
        cannon = (Node3D) getNode("Cannon");
    }

    @RegisterFunction
    @Override
    public void _process(double delta) {
        yaw = gd.lerpAngle(yaw, ship.getYaw(), 0.1);
        pitch = gd.lerpAngle(pitch, ship.getPitch(), 0.1);

        double turretPitch = gd.remap(
            pitch,
            CAMERA_PITCH_MAX,
            CAMERA_PITCH_MIN,
            gd.degToRad(-25),
            gd.degToRad(5)
        );

        double turretYaw = yaw;

        setRotation(new Vector3(0, turretYaw, 0).minus(ship.getRotation()));
        cannon.setRotation((new Vector3((Math.PI / 2.0) + turretPitch, 0, 0)));
        cannon.setRotationOrder(EulerOrder.XYZ);
    }
}

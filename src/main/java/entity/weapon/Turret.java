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
        if (getMultiplayerAuthority() != getMultiplayer().getUniqueId()) return;

        yaw = ship.getYaw();
        pitch = ship.getPitch();

        setRotation(new Vector3(0, yaw, 0).minus(ship.getRotation()));
        cannon.setRotation((new Vector3((Math.PI / 2.0) - pitch, 0, 0)));
        cannon.setRotationOrder(EulerOrder.XYZ);
    }
}

package entity.weapon;

import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.MeshInstance3D;
import godot.api.Node3D;
import godot.api.RigidBody3D;
import godot.api.Timer;
import godot.core.Vector3;
import godot.global.GD;

@RegisterClass
public class Bullet extends RigidBody3D {

    private static final GD gd = GD.INSTANCE;

    @RegisterFunction
    @Override
    public void _process(double delta) {
        if (getGlobalPosition().getY() < -0.5) {
            ((MeshInstance3D) getNode("WaveEffector")).setVisible(false);
        }
    }
}

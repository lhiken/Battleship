package entity.weapon;

import godot.annotation.Export;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.annotation.RegisterProperty;
import godot.annotation.Rpc;
import godot.annotation.RpcMode;
import godot.annotation.Sync;
import godot.api.MeshInstance3D;
import godot.api.Node;
import godot.api.Node3D;
import godot.api.PackedScene;
import godot.api.PhysicsBody3D;
import godot.api.RigidBody3D;
import godot.api.Timer;
import godot.core.StringNames;
import godot.core.Vector3;
import godot.global.GD;

@RegisterClass
public class Bullet extends RigidBody3D {

    private static final GD gd = GD.INSTANCE;

    @RegisterProperty
    @Export
    public PackedScene explosion;

    @RegisterFunction
    @Override
    public void _process(double delta) {
        if (getGlobalPosition().getY() < -0.5) {
            ((MeshInstance3D) getNode("WaveEffector")).setVisible(false);
        }
    }

    @RegisterFunction
    public void onCollision(Node body) {
        spawnExplosion();
    }

    @Rpc(rpcMode = RpcMode.ANY, sync = Sync.SYNC)
    public void spawnExplosion() {
        Node3D explosionNode = (Node3D) explosion.instantiate();
        getParent().addChild(explosionNode);
        explosionNode.setRotation(getLinearVelocity().inverse().normalized());
        explosionNode.setGlobalPosition(getGlobalPosition());
        queueFree();
        rpc(StringNames.toGodotName("spawnExplosion"));
    }
}

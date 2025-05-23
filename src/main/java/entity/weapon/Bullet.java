package entity.weapon;

import entity.Ship;
import godot.annotation.Export;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.annotation.RegisterProperty;
import godot.annotation.Rpc;
import godot.annotation.RpcMode;
import godot.annotation.Sync;
import godot.api.Camera3D;
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
import main.GameCamera;

@RegisterClass
public class Bullet extends RigidBody3D {

    private static final GD gd = GD.INSTANCE;

    private int ownerId = 1;
    private boolean fromBot = false;

    private double timeElapsed = 0;

    private double maxLifetime = 6;

    private Node collisionBody;

    @RegisterProperty
    @Export
    public PackedScene explosion;

    @RegisterFunction
    @Override
    public void _process(double delta) {
        if (getGlobalPosition().getY() < -0.5) {
            ((MeshInstance3D) getNode("WaveEffector")).setVisible(false);
        }

        timeElapsed += delta;

        if (timeElapsed > maxLifetime) spawnExplosion();
    }

    @RegisterFunction
    public void onCollision(Node body) {
        if (!getMultiplayer().isServer()) return;
        collisionBody = body;
        spawnExplosion();
        if (body instanceof Ship) {
            gd.print(ownerId + " hit " + ((Ship) body).getName());
        }
    }

    @RegisterFunction
    public void setOwner(int id) {
        ownerId = id;
    }

    @Rpc(rpcMode = RpcMode.AUTHORITY, sync = Sync.NO_SYNC)
    @RegisterFunction
    public void spawnExplosion() {
        Node3D explosionNode = (Node3D) explosion.instantiate();
        getParent().addChild(explosionNode);
        explosionNode.setRotation(getLinearVelocity().inverse().normalized());
        explosionNode.setGlobalPosition(getGlobalPosition());
        queueFree();

        GameCamera camera = (GameCamera) getParent()
            .getParent()
            .getNode("RenderTarget/Viewport/GameCamera");
        camera.invokeShake(
            // inverse square falloff
            (1 /
                Math.pow(
                    positionProperty().distanceTo(camera.getPosition()),
                    2
                )) *
            100
        );

        rpc(StringNames.toGodotName("spawnExplosion"));
    }
}

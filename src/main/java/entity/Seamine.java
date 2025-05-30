package entity;

import godot.annotation.*;
import godot.api.*;
import godot.core.StringNames;
import godot.core.Vector3;
import godot.global.GD;
import main.GameCamera;
import multiplayer.MultiplayerManager;

@RegisterClass
public class Seamine extends MeshInstance3D {

    private GD gd = GD.INSTANCE;

    private Node collisionBody;
    private int ownerId = 1;
    private Area3D area;
    private CollisionShape3D collision;

    @RegisterProperty
    @Export
    public PackedScene explosion;

    @RegisterFunction
    @Override
    public void _ready() {
        area = (Area3D) getNode("Area3D");
        collision = (CollisionShape3D) getNode("Area3D").getNode("CollisionShape3D");
    }

    @RegisterFunction
    @Override
    public void _process(double delta) {

    }

    @RegisterFunction
    public void bodyEntered(Node body) {
        spawnExplosion();
    }

    @RegisterFunction
    public void onCollision(Node body) {
        if (!getMultiplayer().isServer()) return;
        collisionBody = body;
        spawnExplosion();
        if (body instanceof Ship) {
            MultiplayerManager manager = MultiplayerManager.Instance;
            manager.invokeBulletDamage(ownerId, (Ship) body, 30);
        }
    }

    /**
     * Spawns an explosion through the explosion scene and shakes the camera
     */
    @Rpc(rpcMode = RpcMode.AUTHORITY, sync = Sync.NO_SYNC)
    @RegisterFunction
    public void spawnExplosion() {

        Node3D explosionNode = (Node3D) explosion.instantiate();
        getParent().addChild(explosionNode);
        explosionNode.setRotation(new Vector3(0, Math.PI/2, 0));
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

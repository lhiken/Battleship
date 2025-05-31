package entity;

import godot.annotation.*;
import godot.api.*;
import godot.core.StringNames;
import godot.core.Vector3;
import godot.global.GD;
import main.GameCamera;
import multiplayer.MultiplayerManager;

@RegisterClass
public class Seamine extends Node3D {

    private GD gd = GD.INSTANCE;

    private Node collisionBody;
    private int ownerId = 1;
    private Area3D area;
    private CollisionShape3D collision;
    private boolean justExploded;
    private AudioStreamPlayer explosionSound;

    private double cooldown;
    double globalPosition;

//    @RegisterProperty
//    @Export

    @RegisterFunction
    @Override
    public void _ready() {
        area = (Area3D) getNode("Area3D");
        cooldown = 0;
        collision = (CollisionShape3D) getNode("Area3D").getNode("CollisionShape3D");
        justExploded = true;
        globalPosition = -1;
    }

    @RegisterFunction
    @Override
    public void _process(double delta) {
        if (justExploded) {
            if (getGlobalPosition().getY() > -10) {
                translate(new Vector3(0, -1 * delta, 0));
            }
            cooldown += delta;
        }
        if (cooldown > 20) {
            justExploded = false;
            if (getGlobalPosition().getY() < -1) {
                translate(new Vector3(0, delta, 0));
            }
        }
    }

    @RegisterFunction
    public void bodyEntered(Node3D body) {
        if (cooldown > 20) {
            spawnExplosion();
            if (body instanceof Ship) {
                MultiplayerManager manager = MultiplayerManager.Instance;
                manager.invokeBulletDamage(ownerId, (Ship) body, 30);
            }
            cooldown = 0;
            justExploded = true;
        }
    }

    /**
     * Spawns an explosion through the explosion scene and shakes the camera
     */
    @Rpc(rpcMode = RpcMode.AUTHORITY, sync = Sync.NO_SYNC)
    @RegisterFunction
    public void spawnExplosion() {

        explosionSound = (AudioStreamPlayer)
                getNode("Explosion");
        //        double distance = myShip.getGlobalPosition().minus(this.getGlobalPosition()).length();
        explosionSound.setVolumeDb((float) (0));
        explosionSound.play();

        gd.print("Explosion spawned");

        PackedScene explosion = (PackedScene) gd.load("res://shaders/particles/Prefab.tscn");
        Node3D explosionNode = (Node3D) explosion.instantiate();
        getParent().addChild(explosionNode);
        explosionNode.setRotation(new Vector3(0, Math.PI/2, 0));
        explosionNode.setGlobalPosition(getGlobalPosition());





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
                        500
        );

        rpc(StringNames.toGodotName("spawnExplosion"));

    }


}

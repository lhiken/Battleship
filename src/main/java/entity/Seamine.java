package entity;

import godot.annotation.*;
import godot.api.*;
import godot.core.StringNames;
import godot.core.Vector3;
import godot.global.GD;
import main.GameCamera;
import multiplayer.MultiplayerManager;

/**
 * Class Seamine
 * <p>
 * When a Ship collides with a seamine, the seamine automatically triggers an explosion and then sinks down until floating up again
 * Contains helper methods and methods to spawn explosions
 */
@RegisterClass
public class Seamine extends Node3D {

    private GD gd = GD.INSTANCE;
    private Node collisionBody;
    private int ownerId = -100000; // rayming this is all ur fault
    private Area3D area;
    private CollisionShape3D collision;
    private boolean justExploded;
    private AudioStreamPlayer explosionSound;

    private double cooldown;
    private double globalPosition;

    //    @RegisterProperty
    //    @Export

    /**
     * Overriding Godot's Built-in _ready function
     * Acts as a constructor
     * Gets the child nodes of the seamine and instantiates values
     */
    @RegisterFunction
    @Override
    public void _ready() {
        area = (Area3D) getNode("Area3D");
        cooldown = 0;
        collision = (CollisionShape3D) getNode("Area3D").getNode(
            "CollisionShape3D"
        );
        justExploded = true;
        globalPosition = -1;
    }

    /**
     * Overriding Godot's built-in _process function
     * If the seamine just exploded, sinks down and prepares to explode again
     * The seamine slowly goes up again after a certain amount of time
     * @param delta the time elapsed between each call to _process
     */
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

    /**
     * If another node has entered the Area3D of the seamine, then will trigger an explosion and deal damage
     * @param body The node that has entered the Seamine node
     */
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
        explosionSound = (AudioStreamPlayer) getNode("Explosion");
        //        double distance = myShip.getGlobalPosition().minus(this.getGlobalPosition()).length();
        explosionSound.setVolumeDb((float) (0));
        explosionSound.play();

        PackedScene explosion = (PackedScene) gd.load(
            "res://shaders/particles/Prefab.tscn"
        );
        Node3D explosionNode = (Node3D) explosion.instantiate();
        getParent().addChild(explosionNode);
        explosionNode.setRotation(new Vector3(0, Math.PI / 2, 0));
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

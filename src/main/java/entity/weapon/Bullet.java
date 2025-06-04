package entity.weapon;

import entity.Ship;
import godot.annotation.Export;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.annotation.RegisterProperty;
import godot.annotation.Rpc;
import godot.annotation.RpcMode;
import godot.annotation.Sync;
import godot.api.*;
import godot.core.StringNames;
import godot.core.VariantArray;
import godot.core.Vector3;
import godot.global.GD;
import main.GameCamera;
import multiplayer.MultiplayerManager;

/**
 * The bullet class that manages cannonballs
 * <p>
 *     Is a RigidBody3D, meaning it's movement is controlled through applied forces, impulses, and more
 * </p>
 * <p>
 *     Does not actually handle the movement of the cannonball, that is handled in
 *     match manager in order for better functionality and practice
 *     Handles certain conditions of a cannonball, i.e. a cannonball colliding with a ship/land
 * </p>
 */
@RegisterClass
public class Bullet extends RigidBody3D {

    private static final GD gd = GD.INSTANCE;

    private int ownerId = 1;
    private boolean fromBot = false;

    private double timeElapsed = 0;

    private double maxLifetime = 6;
    private AudioStreamPlayer explosionSound;
    private AudioStreamPlayer explosionSoundMain;
    private AudioStreamPlayer explosionSoundSecondary;

    private Node collisionBody;

    /**
     * The explosion scene which creates particle effects during collisions
     */
    @RegisterProperty
    @Export
    public PackedScene explosion;

    /**
     * Overriding Godot's built-in _process function
     * If its position is more than a certain amount, turn off wave effector
     * Spawns an explosion if its time passes more than a certain amount
     * <p>
     * No need to handle the movement of bullet's here, just use a call to the impulse method to move the bullet!
     * @param delta the time passed between each call to _process
     */
    @RegisterFunction
    @Override
    public void _process(double delta) {
        if (getGlobalPosition().getY() < -0.5) {
            ((MeshInstance3D) getNode("WaveEffector")).setVisible(false);
        }

        timeElapsed += delta;

        if (timeElapsed > maxLifetime) spawnExplosion();
    }

    /**
     * Is called when the cannonball is colliding with something (land, ship, etc)
     * Invokes damage and spawns a collision
     * @param body The body that the cannonball is colliding with
     */
    @RegisterFunction
    public void onCollision(Node body) {
        if (!getMultiplayer().isServer()) return;
        collisionBody = body;
        spawnExplosion();
        if (body instanceof Ship) {
            MultiplayerManager manager = MultiplayerManager.Instance;
            manager.invokeBulletDamage(ownerId, (Ship) body, 15);
        }
    }

    /**
     * Setter method that set's the id of whoever fired the cannonball
     * @param id The id to be set
     */
    @RegisterFunction
    public void setOwner(int id) {
        ownerId = id;
    }

    /**
     * Spawns an explosion through the explosion scene and shakes the camera
     */
    @Rpc(rpcMode = RpcMode.AUTHORITY, sync = Sync.NO_SYNC)
    @RegisterFunction
    public void spawnExplosion() {
        explosionSound = (AudioStreamPlayer) getParent()
            .getParent()
            .getNode("Explosion");
        explosionSoundSecondary = (AudioStreamPlayer) getParent().getParent().getNode("ExplosionSecondary");
        explosionSoundMain = (AudioStreamPlayer) getParent()
                .getParent()
                .getNode("ExplosionMain");

        double distance = 100;
        Ship myShip = (Ship) (getParent().getParent().getNode("Ships").getNode(this.getMultiplayer().getUniqueId() + ""));
        if (myShip != null) {
            distance = myShip.getGlobalPosition().minus(this.getGlobalPosition()).length();
        }
        if (MultiplayerManager.Instance.getPeerId() != ownerId) {
            if (distance > 30) {
                explosionSound.setVolumeDb(-30);
                explosionSound.play();
            } else {
                explosionSoundSecondary.setVolumeDb(-15);
                explosionSoundSecondary.play();
            }
        }
        else {
            explosionSoundMain.setVolumeDb((float) (0));
            explosionSoundMain.play();
        }

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

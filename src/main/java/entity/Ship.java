package entity;

import godot.annotation.Export;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.annotation.RegisterProperty;
import godot.annotation.Rpc;
import godot.annotation.RpcMode;
import godot.annotation.Sync;
import godot.api.*;
import godot.core.NodePath;
import godot.core.StringNames;
import godot.core.Vector3;
import godot.global.GD;
import main.Game;
import main.GameCameraFrame;
import main.MatchManager;
import map.gen.Generator;

/** Ship
 * base ship class nothing works rn please fix :pray:
 */
@RegisterClass
public class Ship extends CharacterBody3D {

    private static final GD gd = GD.INSTANCE;
    private double velocity;
    private double rotation;
    private double turretYaw;
    private double turretPitch;
    private InputState state;

    public AudioStreamPlayer boom;
    public AudioStreamPlayer emptyCannon;

    private double maxVelocity = 5.0;

    @RegisterProperty
    @Export
    public InputProvider provider;

    // pathfinding debug start
    // everything below is useless lmao

    @RegisterProperty
    @Export
    public Generator gen;

    private int frameCounter = 0;

    private double cooldownTime = 2;
    private double cooldownPercent = 1;

    private double health = 100;

    // private MeshInstance3D trajectoryMesh;

    private boolean sinking = false;

    @RegisterFunction
    @Override
    public void _ready() {
        setMultiplayerAuthority(Integer.parseInt(getName().toString()));
        health = 100;
        emptyCannon = (AudioStreamPlayer) getNode("EmptyCannon");
        // instantiateNewCannon();
        boom = (AudioStreamPlayer) getNode("CannonFire");
    }

    @RegisterFunction
    @Override
    public void _process(double delta) {
        frameCounter++;

        cooldownPercent += delta / cooldownTime;
        cooldownPercent = gd.clamp(cooldownPercent, 0, 1);

        if (health <= 0 && !sinking) {
            sinking = true;
        }

        if (sinking) {
            globalTranslate(
                new Vector3(
                    0,
                    (getGlobalPosition().getY() * 0.1 - 0.5) * delta * 0.25,
                    0
                )
            );

            velocity = gd.lerp(velocity, 0, 0.05);

            if (getGlobalPosition().getY() < -30) {
                queueFree();
            }

            sinking = true;

            return;
        }

        if (!isMultiplayerAuthority()) {
            return;
        }

        turretYaw = gd.lerpAngle(turretYaw, state.getYaw(), 0.1);
        turretPitch = gd.lerpAngle(turretPitch, state.getPitch(), 0.1);

        Vector3 position = this.getGlobalPosition();
        position.setY(position.getY() + 3);

        if (provider != null) {
            state = provider.getState();

            if (state.getEmittedAction() != -1 && cooldownPercent == 1) {
                MatchManager matchManager = (MatchManager) getParent()
                    .getParent();
                Vector3 origin =
                    ((Node3D) getNode(
                            "Turret/Cannon/ProjectileOrigin"
                        )).getGlobalPosition();

                matchManager.rpc(
                    StringNames.toGodotName("spawnBullet"),
                    getMultiplayer().getUniqueId(),
                    new Vector3(
                        Math.sin(state.getYaw()),
                        Math.sin(state.getPitch()),
                        Math.cos(state.getYaw())
                    ).normalized(),
                    origin,
                    getVelocity()
                );
                // gd.print(state.getPower());
                // cannon.instantiateNewBullet(
                //     this.getRotation(),
                //     velocity + state.getPower()
                // );

                // uncommenting this before fixing it will cause process to terminate early
                // resulting in cooldown never being reset and firing forever!!
                // boom.play();
                cooldownPercent = 0;
            } else if (state.getEmittedAction() != -1 && cooldownPercent < 1) {
                // emptyCannon.play();
            }
        }
        // drawProjectilePath();
    }

    @Rpc(rpcMode = RpcMode.AUTHORITY, sync = Sync.NO_SYNC)
    @RegisterFunction
    public void setHealth(double health) {
        this.health = health;
        rpc(StringNames.toGodotName("setHealth"), health);
    }

    @RegisterFunction
    public double getHealth() {
        return health;
    }

    @RegisterFunction
    public double getPitch() {
        return turretPitch;
    }

    @RegisterFunction
    public double getYaw() {
        return turretYaw;
    }

    public InputState getState() {
        return provider.getState();
    }

    @RegisterFunction
    @Override
    public void _physicsProcess(double delta) {
        if (isMultiplayerAuthority() && !sinking) {
            if (provider == null) return;

            state = provider.getState();

            double targetVelocity = state.getVelocity() * maxVelocity;
            double targetRotation = state.getRotation();

            velocity = gd.lerp(velocity, targetVelocity, 0.1);
            rotation = gd.lerpAngle(rotation, targetRotation, 0.1);

            Vector3 direction = new Vector3(
                Math.sin(rotation),
                0,
                Math.cos(rotation)
            );

            double time = frameCounter / 40.0;
            double rockX = Math.sin(time * 0.7) * 0.025;
            double rockZ = Math.sin(time * 0.9) * 0.015;
            Vector3 rockingOffset = new Vector3(rockX, 0, rockZ);

            setPosition(
                new Vector3(getPosition().getX(), 0, getPosition().getZ())
            );
            setVelocity(direction.times(velocity));
            setRotation(new Vector3(0, rotation, 0).plus(rockingOffset));
        }
        moveAndSlide();
    }

    @RegisterFunction
    public void setProvider(InputProvider provider) {
        this.provider = provider;
    }

    @RegisterFunction
    public double getCooldownPercentage() {
        return cooldownPercent;
    }
}

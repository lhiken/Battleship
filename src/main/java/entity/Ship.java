package entity;

import entity.weapon.Weapon;
import godot.annotation.Export;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.annotation.RegisterProperty;
import godot.api.*;
import godot.api.BaseMaterial3D.DepthDrawMode;
import godot.api.BaseMaterial3D.ShadingMode;
import godot.api.GeometryInstance3D.ShadowCastingSetting;
import godot.api.Mesh.PrimitiveType;
import godot.core.Color;
import godot.core.NodePath;
import godot.core.PackedInt32Array;
import godot.core.PackedVector3Array;
import godot.core.StringName;
import godot.core.StringNames;
import godot.core.VariantArray;
import godot.core.VariantType;
import godot.core.Vector2;
import godot.core.Vector3;
import godot.global.GD;
import java.lang.reflect.Array;
import java.util.ArrayList;
import main.MatchManager;
import map.gen.Coordinate;
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

    @RegisterProperty
    @Export
    public AudioStreamPlayer3D boom;

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

    public double cooldownTime = 2;
    public double cooldownPercent = 1;

    // only for visualization
    public double projectilePathTimestep = 0.1;
    public double projectilePathSpeed = 25.0;

    // private MeshInstance3D trajectoryMesh;

    @RegisterFunction
    @Override
    public void _ready() {
        setMultiplayerAuthority(Integer.parseInt(getName().toString()));
        // instantiateNewCannon();
        // boom = (AudioStreamPlayer3D) getNode(new NodePath("CannonFire"));
    }

    @RegisterFunction
    @Override
    public void _process(double delta) {
        frameCounter++;

        cooldownPercent += delta / cooldownTime;
        cooldownPercent = gd.clamp(cooldownPercent, 0, 1);

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
                if (boom != null) {
                    gd.print(boom);
                    boom.play();
                }

                cooldownPercent = 0;
            }
        }
        // drawProjectilePath();
    }

    // @RegisterFunction
    // public void drawProjectilePath() {
    //     ArrayList<Vector3> points = new ArrayList<>();

    //     if (trajectoryMesh != null) {
    //         trajectoryMesh.queueFree();
    //         trajectoryMesh = null;
    //     }

    //     for (double t = 0; t < 5.0; t += projectilePathTimestep) {
    //         Vector3 newPoint = getProjectilePosition(
    //             ((Node3D) getNode(
    //                     "Turret/Cannon/ProjectileOrigin"
    //                 )).getGlobalPosition(),
    //             new Vector3(
    //                 Math.sin(turretYaw),
    //                 Math.sin(turretPitch),
    //                 Math.cos(turretYaw)
    //             )
    //                 .normalized()
    //                 .times(projectilePathSpeed)
    //                 .plus(velocityProperty()),
    //             t
    //         );
    //         points.add(newPoint);
    //     }

    //     trajectoryMesh = createTrajectoryMesh(points);
    //     getTree().getRoot().addChild(trajectoryMesh);
    // }

    // @RegisterFunction
    // public Vector3 getProjectilePosition(
    //     Vector3 shipPos,
    //     Vector3 vi,
    //     double time
    // ) {
    //     return vi
    //         .times(time)
    //         .plus(new Vector3(0, -9.8, 0).times(Math.pow(time, 2)).times(0.5))
    //         .plus(shipPos);
    // }

    // @RegisterFunction
    // public MeshInstance3D createTrajectoryMesh(ArrayList<Vector3> points) {
    //     if (points.size() < 2) return null;

    //     MeshInstance3D meshInstance = new MeshInstance3D();
    //     SurfaceTool surfaceTool = new SurfaceTool();

    //     surfaceTool.begin(Mesh.PrimitiveType.LINES);

    //     for (int i = 0; i < points.size() - 1; i++) {
    //         surfaceTool.setColor(new Color(0.5, 0.5, 0.5, 0.8));
    //         surfaceTool.addVertex(points.get(i));

    //         surfaceTool.setColor(new Color(0.5, 0.5, 0.5, 0.8));
    //         surfaceTool.addVertex(points.get(i + 1));
    //     }

    //     ArrayMesh mesh = surfaceTool.commit();

    //     ORMMaterial3D material = new ORMMaterial3D();
    //     material.setShadingMode(ShadingMode.UNSHADED);
    //     material.setAlbedo(
    //         new Color(cooldownPercent, cooldownPercent, cooldownPercent, 1.0)
    //     );

    //     meshInstance.setMesh(mesh);
    //     meshInstance.setMaterialOverride(material);
    //     meshInstance.setCastShadowsSetting(ShadowCastingSetting.OFF);

    //     return meshInstance;
    // }

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
        if (isMultiplayerAuthority()) {
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

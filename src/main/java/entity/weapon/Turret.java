package entity.weapon;

import entity.Ship;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.*;
import godot.api.BaseMaterial3D.ShadingMode;
import godot.api.BaseMaterial3D.Transparency;
import godot.core.Color;
import godot.core.EulerOrder;
import godot.core.Transform3D;
import godot.core.Vector3;
import godot.global.GD;
import java.util.ArrayList;
import javax.sound.sampled.Line;

@RegisterClass
public class Turret extends MeshInstance3D {

    private GD gd = GD.INSTANCE;
    private Ship ship;
    private Node3D cannon;
    private boolean showAim;

    private double yaw;
    private double pitch;

    @RegisterFunction
    @Override
    public void _ready() {
        ship = (Ship) getParent();
        cannon = (Node3D) getNode("Cannon");
        showAim = true;
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

        if (showAim) {
            Vector3 velocity =
                (new Vector3(
                        Math.sin(ship.getYaw()),
                        Math.sin(ship.getPitch()),
                        Math.cos(ship.getYaw())
                    )).normalized()
                    .times(25);
            // gd.print(velocity);
            Vector3 pos =
                ((Node3D) getNode(
                        "Cannon/ProjectileOrigin"
                    )).getGlobalPosition();

            MeshInstance3D aimCurve = (MeshInstance3D) getNode(
                "Cannon/ProjectileOrigin/MeshInstance3D"
            );
            ImmediateMesh mesh = new ImmediateMesh();
            aimCurve.setMesh(mesh);

            mesh.surfaceBegin(Mesh.PrimitiveType.LINE_STRIP);
            ORMMaterial3D material =
                (ORMMaterial3D) aimCurve.getMaterialOverride();
            material.setAlbedo(
                new Color(
                    ship.getCooldownPercentage(),
                    ship.getCooldownPercentage(),
                    ship.getCooldownPercentage(),
                    ship.getCooldownPercentage()
                )
            );

            double step = 0.05;
            double simTime = 0.0;
            double maxTime = 10.0;

            while (pos.getY() >= 0 && simTime <= maxTime) {
                simTime += step;

                Transform3D meshTransform = aimCurve.getGlobalTransform();
                Vector3 localPos = meshTransform
                    .affineInverse()
                    .xform$godot_core_library(pos);
                mesh.surfaceAddVertex(localPos);

                velocity.setY(velocity.getY() - (9.8 * step));
                pos = pos.plus(velocity.times(step));
            }

            mesh.surfaceEnd();
        }
    }

    public void setShowAim(boolean var) {
        showAim = var;
    }
}

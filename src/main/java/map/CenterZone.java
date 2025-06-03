package map;

import entity.Ship;
import entity.providers.BotProvider;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.*;
import godot.core.StringName;
import godot.core.Vector3;
import godot.global.GD;
import multiplayer.MultiplayerManager;

@RegisterClass
public class CenterZone extends StaticBody3D {

    private static final GD gd = GD.INSTANCE;

    private double intensity;
    private boolean entered;
    private double time;
    private double pointTimer;
    private Vector3 whiteness;
    private double peerId;
    private AudioStreamPlayer soundEffect;

    @RegisterFunction
    @Override
    public void _ready() {
        time = 0;
        intensity = 1;
        entered = false;
        whiteness = new Vector3(0, 0, 0);
        MeshInstance3D area = (MeshInstance3D) getNode("MeshInstance3D2");
        Material material = area.getActiveMaterial(0);
        material.set("shader_parameter/intensity", intensity);
    }
    @RegisterFunction
    @Override
    public void _process(double delta) {
        gd.print(entered);
        if (entered) {
            pointTimer += delta;
            time += delta;
            MeshInstance3D area = (MeshInstance3D) getNode("MeshInstance3D2");
            Material material = area.getActiveMaterial(0);
            if (time < 0.2) {
                intensity = gd.lerp(intensity, 3, 0.45);
                whiteness.setX(gd.lerp(whiteness.getX(), 5, 0.05));
                whiteness.setY(gd.lerp(whiteness.getY(), 5, 0.05));
                whiteness.setZ(gd.lerp(whiteness.getZ(), 5, 0.05));

            }
            else {
                intensity = gd.lerp(intensity, 1, 0.005);
                whiteness.setX(gd.lerp(whiteness.getX(), 0, 0.05));
                whiteness.setY(gd.lerp(whiteness.getY(), 0, 0.05));
                whiteness.setZ(gd.lerp(whiteness.getZ(), 0, 0.05));
            }
            if (pointTimer > 1) {
                MultiplayerManager multiplayerManager = MultiplayerManager.Instance.updatePoints();
                pointTimer = 0;
            }
            material.set("shader_parameter/intensity", intensity);
            material.set("shader_parameter/whiteness", whiteness);
        }
        else if (!entered) {
            pointTimer = 0;
        }
        else if (intensity != 1) {
            MeshInstance3D area = (MeshInstance3D) getNode("MeshInstance3D2");
            Material material = area.getActiveMaterial(0);
            intensity = gd.lerp(intensity, 1, 0.005);
            material.set("shader_parameter/intensity", intensity);
            time = 0;
        }
    }

    @RegisterFunction
    public void bodyEntered(Node3D body) {
        gd.print("Body ENTERED");
        if (body instanceof Ship) {
            String name = ((Ship) body).getName().toString();
            if (!(((Ship) body).getProvider() instanceof BotProvider)) {
                if (Integer.parseInt(name) == ((Ship) body).getMultiplayer().getUniqueId()) {
                    peerId = Integer.parseInt(name);
                    entered = true;
                    soundEffect = (AudioStreamPlayer) getNode("EnterSound");
                    soundEffect.play();
                }
            }
        }
    }

    @RegisterFunction
    public void bodyExited(Node3D body) {
        gd.print("Body LEFT");
        if (body instanceof Ship) {
            String name = ((Ship) body).getName().toString();
            if (!(((Ship) body).getProvider() instanceof BotProvider)) {
                if (Integer.parseInt(name) == peerId) {
                    entered = false;
                }
            }
        }
    }
}

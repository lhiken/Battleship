package main;

import godot.annotation.Export;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.annotation.RegisterProperty;
import godot.api.Camera3D;
import godot.api.Input;
import godot.api.Input.MouseMode;
import godot.api.Node3D;
import godot.api.TextureRect;
import godot.core.Basis;
import godot.core.EulerOrder;
import godot.core.Vector3;
import godot.global.GD;

@RegisterClass
public class GameCamera extends Camera3D {

    private GD gd = GD.INSTANCE;

    @RegisterProperty
    @Export
    public Node3D shipNode;

    @RegisterProperty
    @Export
    public TextureRect cameraParent;

    private Vector3 shipPosition = new Vector3();

    private double cameraVerticalOffset = 1.0;
    private double cameraDistance = 10.0;

    private double yaw = 0.0;
    private double pitch = Math.toRadians(15);

    @RegisterFunction
    @Override
    public void _ready() {
        Input.setMouseMode(MouseMode.CAPTURED);
        setCurrent(true);
    }

    @RegisterFunction
    @Override
    public void _process(double delta) {
        if (shipNode == null || cameraParent == null) return;

        GameCameraFrame frame = (GameCameraFrame) cameraParent;

        // this is really stupid and input should be handled here but
        // theres a bug or smth its prolly intended behavior but i dont
        // want to deal with it so too bad
        yaw = frame.getYaw();
        pitch = frame.getPitch();
        double tCameraDistance = frame.getZoom();

        cameraDistance +=
            (tCameraDistance - cameraDistance) * Math.min(1.0, 5.0 * delta);

        shipPosition = shipNode.getGlobalPosition();

        Vector3 focusPoint = shipPosition.plus(
            new Vector3(0, cameraVerticalOffset, 0)
        );
        Basis rotationBasis = Basis.Companion.fromEuler(
            new Vector3(pitch, yaw, 0.0),
            EulerOrder.YXZ
        );
        Vector3 offset = rotationBasis
            .getZ()
            .normalized()
            .times(-cameraDistance);

        setGlobalPosition(focusPoint.plus(offset));
        lookAt(focusPoint, new Vector3(0, 1, 0));
    }

    @RegisterFunction
    public void invokeShake(double strength) {}
}

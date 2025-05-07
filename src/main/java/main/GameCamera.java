package main;

import godot.annotation.Export;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.annotation.RegisterProperty;
import godot.api.Camera3D;
import godot.api.Input;
import godot.api.Input.MouseMode;
import godot.api.InputEvent;
import godot.api.InputEventMouseMotion;
import godot.api.Node3D;
import godot.core.Basis;
import godot.core.EulerOrder;
import godot.core.Vector3;

@RegisterClass
public class GameCamera extends Camera3D {

    @RegisterProperty
    @Export
    public Node3D shipNode;

    private Vector3 shipPosition = new Vector3();

    private double minInclinationDeg = 5.0;
    private double maxInclinationDeg = 50.0;

    private double cameraVerticalOffset = 1.0;
    private double cameraDistance = 10.0;

    private double mouseSensitivity = 0.005; // better scale
    private double yaw = 0.0;
    private double pitch = Math.toRadians(15);

    @RegisterFunction
    @Override
    public void _ready() {
        Input.setMouseMode(MouseMode.CAPTURED);
    }

    @RegisterFunction
    @Override
    public void _input(InputEvent event) {
        if (event.isActionPressed("show_cursor")) {
            Input.setMouseMode(MouseMode.VISIBLE);
        } else if (event.isActionReleased("show_cursor")) {
            Input.setMouseMode(MouseMode.CAPTURED);
        }
    }

    @RegisterFunction
    @Override
    public void _unhandledInput(InputEvent event) {
        if (
            event instanceof InputEventMouseMotion &&
            Input.getMouseMode() == MouseMode.CAPTURED
        ) {
            InputEventMouseMotion motion = (InputEventMouseMotion) event;
            yaw -= motion.getRelative().getX() * mouseSensitivity;
            pitch -= motion.getRelative().getY() * mouseSensitivity;

            double minInclination = Math.toRadians(minInclinationDeg);
            double maxInclination = Math.toRadians(maxInclinationDeg);

            pitch = Math.max(minInclination, Math.min(maxInclination, pitch));
        }
    }

    @RegisterFunction
    @Override
    public void _process(double delta) {
        if (shipNode == null) return;

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

    public void invokeShake(double strength) {
        // Placeholder for screen shake
    }
}

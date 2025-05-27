package main;

import entity.Ship;
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

    private boolean playerMode = false;

    private double time = 0.0;

    private double shakeStrength = 0.0;
    private double shakeDuration = 0.0;
    private double shakeTimer = 0.0;
    private Vector3 shakeOffset = new Vector3();
    private double shakeFrequency = 30.0;

    @RegisterFunction
    @Override
    public void _ready() {
        setCurrent(true);
    }

    @RegisterFunction
    @Override
    public void _process(double delta) {
        time += delta;

        if (shakeTimer < shakeDuration) {
            shakeTimer += delta;
        }

        if (!playerMode) {
            setGlobalPosition(
                new Vector3(
                    40 + Math.cos(time * 0.6 + 0.2) * 0.25,
                    8.0 + Math.sin(time * 1.25) * 0.25,
                    -40 + Math.sin(time + 0.75) * 0.25
                )
            );
            lookAt(new Vector3(0, 0, 0), new Vector3(0, 1, 0));
            return;
        }

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

        updatePosition(focusPoint, offset);
    }

    private void updatePosition(Vector3 focusPoint, Vector3 offset) {
        Vector3 finalPosition = focusPoint.plus(offset);

        if (shakeTimer < shakeDuration) {
            double intensity =
                shakeStrength * (1.0 - (shakeTimer / shakeDuration));

            double shakeX =
                Math.sin(shakeTimer * shakeFrequency) * intensity * 0.1;
            double shakeY =
                Math.sin(shakeTimer * shakeFrequency * 1.3 + 1.0) *
                intensity *
                0.15;
            double shakeZ =
                Math.sin(shakeTimer * shakeFrequency * 0.8 + 2.0) *
                intensity *
                0.02;

            shakeOffset = new Vector3(shakeX, shakeY, shakeZ);
            finalPosition = finalPosition.plus(shakeOffset);
        } else {
            shakeOffset = new Vector3();
        }

        setGlobalPosition(finalPosition);
        lookAt(focusPoint, new Vector3(0, 1, 0));
    }

    @RegisterFunction
    public void setPlayerMode() {
        Input.setMouseMode(MouseMode.CAPTURED);

        playerMode = true;
        setFov(90f);
    }

    @RegisterFunction
    public void setSpectatorMode() {
        Input.setMouseMode(MouseMode.MAX);

        playerMode = false;
        setFov(45f);
    }

    @RegisterFunction
    public void setShip(Ship ship) {
        this.shipNode = ship;
    }

    @RegisterFunction
    public void invokeShake(double strength) {
        this.shakeStrength = strength;
        this.shakeDuration = Math.min(strength * 0.8, 3.0);
        this.shakeTimer = 0.0;
        this.shakeFrequency = Math.random() * 10.0 + 20.0;
    }
}

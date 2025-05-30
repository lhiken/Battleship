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
import godot.api.Panel;
import godot.api.TextureRect;
import godot.core.Basis;
import godot.core.Color;
import godot.core.EulerOrder;
import godot.core.Vector3;
import godot.global.GD;
import ui.lobby.Lobby;

/**
 * The GameCamera class
 * Displays a different camera depending on which ship it is
 * <p>
 * Includes methods for certain conditions where the camera behavior changes such as shaking
 * Updates its position based on certain values
 * Contains its respective setter and getter methods
 */
@RegisterClass
public class GameCamera extends Camera3D {

    private GD gd = GD.INSTANCE;

    /**
     * A reference Node to its respective ship
     */
    @RegisterProperty
    @Export
    public Ship shipNode;

    /**
     * Determines the pixelation of the camera
     */
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

    private boolean sank = false;
    private boolean refreshed = false;

    private Panel panel = null;

    /**
     * Overrides Godot's built-in _ready function
     * Sets the current value to true
     * Acts as a constructor
     */
    @RegisterFunction
    @Override
    public void _ready() {
        setCurrent(true);
    }

    /**
     * Overrides Godot's built-in _process function
     * Changes the camera's view based on certain conditions
     * @param delta the time elapsed between each call to _process
     */
    @RegisterFunction
    @Override
    public void _process(double delta) {
        if (panel == null) {
            panel = (Panel) getParent().getParent().getNode("RespawnPanel");

            panel.setVisible(false);
        }

        time += delta;

        if (shakeTimer < shakeDuration) {
            shakeTimer += delta;
        }

        if (sank || (shipNode != null && shipNode.isSinking())) {
            if (!sank) time = 0;
            sank = true;

            panel.setVisible(true);
            panel.setZIndex(100);

            double fadeDuration = 4.0;
            double alpha = time < 1
                ? gd.smoothstep(0, 1, time)
                : time > fadeDuration - 1
                    ? gd.smoothstep(1, 0, 1.0 - (fadeDuration - time))
                    : 1;

            panel.setModulate(new Color(panel.getModulate(), alpha));

            if (time > fadeDuration / 2.0 && !refreshed) {
                setSpectatorMode();
                Lobby lobby =
                    ((Lobby) getParent() // tung
                            .getParent() // tung
                            .getParent() // tung
                            .getNode("Lobby")); // i hate this and id love to use signals but unfortunately we dont have time
                lobby.setVisible(true);
                shipNode = null;
                Input.setMouseMode(MouseMode.VISIBLE);

                refreshed = true;
            } else if (time > fadeDuration) {
                sank = false;
                Lobby lobby =
                    ((Lobby) getParent() // tung
                            .getParent() // tung
                            .getParent() // tung
                            .getNode("Lobby"));
                lobby.setZIndex(100);
                panel.setZIndex(-100);
                return;
            }

            return;
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

        if (((Ship) shipNode).isSinking()) shipPosition.setY(0);

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

    /**
     * Update the position of the camera depending on the values in the parameters
     * @param focusPoint The focus point of the camera
     * @param offset The offset of the camera
     */
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

    /**
     * Sets the mouse to be captured in the middle of the screen
     */
    @RegisterFunction
    public void setPlayerMode() {
        Input.setMouseMode(MouseMode.CAPTURED);

        playerMode = true;
        setFov(90f);
    }

    /**
     * Sets the mouse to be in spectator mode
     */
    @RegisterFunction
    public void setSpectatorMode() {
        Input.setMouseMode(MouseMode.MAX);

        playerMode = false;
        setFov(45f);
    }

    /**
     * Setter method for which ship the camera follows
     * @param ship the ship for the camera to follow
     */
    @RegisterFunction
    public void setShip(Ship ship) {
        this.shipNode = ship;
        if (ship != null) {
            this.sank = false;
            this.refreshed = false;
            this.time = 0.0;
            if (panel != null) {
                panel.setVisible(false);
                setPlayerMode();
            }
        }
    }

    /**
     * Shaking of the camera during collision with cannonball
     * @param strength The severity of the shake
     */
    @RegisterFunction
    public void invokeShake(double strength) {
        this.shakeStrength = strength;
        this.shakeDuration = Math.min(strength * 0.8, 3.0);
        this.shakeTimer = 0.0;
        this.shakeFrequency = Math.random() * 10.0 + 20.0;
    }
}

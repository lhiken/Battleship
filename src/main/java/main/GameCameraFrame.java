package main;

import godot.annotation.Export;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.annotation.RegisterProperty;
import godot.api.Camera3D;
import godot.api.Input;
import godot.api.Input.MouseMode;
import godot.api.InputEvent;
import godot.api.InputEventMouseButton;
import godot.api.InputEventMouseMotion;
import godot.api.SubViewport;
import godot.api.TextureRect;
import godot.core.MouseButton;
import godot.core.Vector2;
import godot.core.Vector2i;
import godot.global.GD;

/**
 * The GameCameraFrame class
 * The class for the textureRect that renders the main scene
 */
@RegisterClass
public class GameCameraFrame extends TextureRect {

    // include this line everywhere bc godot's methods use it
    private static final GD gd = GD.INSTANCE;
    private SubViewport viewport;
    private Vector2 windowSize;
    private double aspectRatio;

    /**
     * The camera that needs to be rendered for the main scene
     */
    @Export
    @RegisterProperty
    public Camera3D camera;

    /**
     * The max height
     */
    @Export
    @RegisterProperty
    public int maxHeight;

    /**
     * The minimum height
     */
    @Export
    @RegisterProperty
    public int minHeight;

    private double mouseSensitivity = 0.005;
    private double zoomSensitivity = 2.0;

    private double minInclinationDeg = 5.0;
    private double maxInclinationDeg = 50.0;

    private double yaw = 0.0;
    private double pitch = Math.toRadians(15);

    private double maxCameraDistance = 16.0;
    private double minCameraDistance = 6.0;
    private double cameraDistance = 10.0;

    private MouseMode prevMode;

    /**
     * Overrides Godot's built-in _ready function
     * Acts as a constructor
     * Sets texture and current, prepares for the camera
     */
    @RegisterFunction
    @Override
    public void _ready() {
        viewport = (SubViewport) getNodeOrNull("Viewport");

        updateViewport();

        setTexture(viewport.getTexture());

        camera.getParent().removeChild(camera);
        viewport.addChild(camera);
        camera.setCurrent(true);
    }

    /**
     * Overrides Godot's built-in _input function
     * Determines the mouse visibility and movement depending on input by the player
     * @param event the key that was pressed or event that occurred
     */
    @RegisterFunction
    @Override
    public void _input(InputEvent event) {
        if (event.isActionPressed("show_cursor")) {
            prevMode = Input.getMouseMode();
            Input.setMouseMode(MouseMode.VISIBLE);
        } else if (event.isActionReleased("show_cursor")) {
            Input.setMouseMode(MouseMode.CAPTURED);
        }
    }

    /**
     * Overrides Godot's built-in _unhandledInput function
     * Determines more orientation of the camera depending on input
     * @param event the key that was pressed or event that occurred
     */
    @RegisterFunction
    @Override
    public void _unhandledInput(InputEvent event) {
        if (
            event instanceof InputEventMouseMotion &&
            Input.getMouseMode() == MouseMode.CAPTURED
        ) {
            InputEventMouseMotion motion = (InputEventMouseMotion) event;
            yaw -= motion.getRelative().getX() * mouseSensitivity;
            pitch += motion.getRelative().getY() * mouseSensitivity;

            double minInclination = Math.toRadians(minInclinationDeg);
            double maxInclination = Math.toRadians(maxInclinationDeg);

            pitch = Math.max(minInclination, Math.min(maxInclination, pitch));
        }

        if (event instanceof InputEventMouseButton) {
            if (
                ((InputEventMouseButton) event).getButtonIndex() ==
                MouseButton.WHEEL_UP
            ) {
                cameraDistance -= zoomSensitivity;
            } else if (
                ((InputEventMouseButton) event).getButtonIndex() ==
                MouseButton.WHEEL_DOWN
            ) {
                cameraDistance += zoomSensitivity;
            }

            cameraDistance = gd.clamp(
                cameraDistance,
                minCameraDistance,
                maxCameraDistance
            );
        }
    }

    /**
     * Overrides Godot's built-in _process function
     * Re-renders a viewpoint
     * @param delta the time elapsed between each call to _process
     */
    @RegisterFunction
    @Override
    public void _process(double delta) {
        updateViewport();
    }

    private void updateViewport() {
        int targetHeight = (int) gd.lerp(
            minHeight,
            maxHeight,
            cameraDistance / maxCameraDistance
        );

        windowSize = getViewport().getVisibleRect().getSize();
        aspectRatio = windowSize.getX() / windowSize.getY();
        int targetWidth = Math.round((int) (targetHeight * aspectRatio));
        viewport.setSize(new Vector2i(targetWidth, targetHeight));
    }

    // incredibly funny functions to get around funny bug that im too lazy to google

    /**
     * Getter method for the pitch of the camera
     * @return The rotation of the camera around the x-axis
     */
    public double getPitch() {
        return pitch;
    }

    /**
     * Getter method for the yaw of the camera
     * @return The rotation of the camera around the y-axis
     */
    public double getYaw() {
        return yaw;
    }

    /**
     * Getter method for the zoom of the camera
     * @return The zoom of the camera
     */
    public double getZoom() {
        return cameraDistance;
    }
}

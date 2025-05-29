package entity.providers;

import entity.InputProvider;
import entity.InputState;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.*;
import godot.api.Input.MouseMode;
import godot.core.Vector2;
import godot.global.GD;
import main.GameCameraFrame;

/**
 * PlayerProvider class that extends InputProvider
 * a version of InputProvider for the local player's input
 * <p>
 * Passes player input such as moving the camera, keyboard inputs, and more into an InputState which is
 * eventually used by ship to determine it's orientation and actions
 * <p>
 * Process method finds input and calculates values for inputState
 * Getter and setter methods for a ship's respective inputState
 */
@RegisterClass
public class PlayerProvider extends InputProvider {

    private static final GD gd = GD.INSTANCE;

    private final double VELOCITY_STEP = 3.0;
    private final double ROTATION_STEP = 6.0;
    private double rotation;
    private double velocity;
    private int selectedAction;
    private boolean emitAction;
    private double power;
    private InputState currentState;

    private AudioStreamPlayer wind;
    private double volume;

    private double turretPitch;
    private double turretYaw;

    /**
     * Overrides Godot's internal built-in _ready function
     * Runs upon being instantiated in the game world and acts as a constructor
     */
    @RegisterFunction
    @Override
    public void _ready() {
        gd.print("Loaded player provider");
        currentState = new InputState();
        rotation = 0;
        velocity = 0;
        power = 1;
        selectedAction = 1;
        emitAction = false;
        wind = (AudioStreamPlayer) getParent().getNode("WindBlowing");
        wind.autoplayProperty(true);
        wind.play();
        volume = 0;
    }

    /**
     * Overrides Godot's internal built-in _process function
     * Runs every frame to process user input and turn it into values in inputState
     * @param delta The time elapsed between each call to _process
     */
    @RegisterFunction
    @Override
    public void _process(double delta) {
        if (!wind.isPlaying()) {
            wind.play();
        }

        if (velocity == 1) {
            Camera3D camera = (Camera3D) getParent()
                .getParent()
                .getParent()
                .getNode("RenderTarget/Viewport/GameCamera");
            float fov = camera.getFov();
            float newFov = (float) gd.lerp(fov, 55f, 0.005);
            camera.setFov(newFov);
        } else {
            Camera3D camera = (Camera3D) getParent()
                .getParent()
                .getParent()
                .getNode("RenderTarget/Viewport/GameCamera");
            float fov = camera.getFov();
            float newFov = (float) gd.lerp(fov, 45f, 0.01);
            camera.setFov(newFov);
        }

        Vector2 inputDirection = Input.getVector(
            "backward",
            "forward",
            "right",
            "left"
        );
        if (inputDirection.getX() != 0) {
            volume = gd.lerp(wind.getVolumeDb(), -15, 0.01);
            wind.setVolumeDb((float) volume);
            velocity += inputDirection.getX() * VELOCITY_STEP * delta;
            velocity = gd.clamp(velocity, -0.5, 1);
        } else {
            if (velocity > 0) {
                velocity -= (VELOCITY_STEP / 5) * delta;
            } else if (velocity < 0) {
                velocity += (VELOCITY_STEP / 5) * delta;
            }
            volume = gd.lerp(wind.getVolumeDb(), -80, 0.002);
            wind.setVolumeDb((float) volume);
        }
        if (inputDirection.getY() != 0) {
            rotation += (inputDirection.getY() * ROTATION_STEP * delta) / 3;
        }

        //        if (inputDirection.isZeroApprox()) {
        //            if (velocity > 0) {
        //                velocity -= (VELOCITY_STEP / 5) * delta;
        //            } else if (velocity < 0) {
        //                velocity += (VELOCITY_STEP / 5) * delta;
        //            }
        //        } else {
        //            rotation += (inputDirection.getY() * ROTATION_STEP * delta) / 3;
        //            velocity += inputDirection.getX() * VELOCITY_STEP * delta;
        //            velocity = gd.clamp(velocity, -0.5, 1);
        //        }

        if (Input.isActionJustPressed("one")) {
            selectedAction = 1;
        } else if (Input.isActionJustPressed("two")) {
            selectedAction = 2;
        }

        if (Input.isActionPressed("space")) {
            power += delta;
        } else if (Input.isActionJustReleased("space")) {
            emitAction = true;
        } else {
            emitAction = false;
            power = 0;
        }

        GameCameraFrame frame = (GameCameraFrame) getParent()
            .getParent()
            .getParent()
            .getNode("RenderTarget");

        double framePitch = frame.getPitch();
        double frameYaw = frame.getYaw();

        turretYaw = frameYaw;
        turretPitch = gd.clamp(framePitch, gd.degToRad(10), gd.degToRad(40));

        turretPitch = gd.remap(
            turretPitch,
            gd.degToRad(10),
            gd.degToRad(40),
            gd.degToRad(20),
            gd.degToRad(-10)
        );

        updateState();
    }

    private void updateState() {
        currentState.velocity = velocity;
        currentState.rotation = rotation;
        currentState.emitAction = emitAction ? selectedAction : -1;
        currentState.power = Math.min(14, power * 3);
        currentState.turretYaw = turretYaw;
        currentState.turretPitch = turretPitch;
    }

    /**
     * Getter method for the inputState
     * @return it's inputState
     */
    @RegisterFunction
    @Override
    public InputState getState() {
        return currentState;
    }
}

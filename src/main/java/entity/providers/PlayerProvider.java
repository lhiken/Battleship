package entity.providers;

import entity.InputProvider;
import entity.InputState;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.Input;
import godot.api.Input.MouseMode;
import godot.api.InputEvent;
import godot.api.InputEventMouseMotion;
import godot.core.Vector2;
import godot.global.GD;
import main.GameCameraFrame;

/** PlayerProvider
 * a version of inputprovider for the local player's input
 */
@RegisterClass
public class PlayerProvider extends InputProvider {

    private static final GD gd = GD.INSTANCE;

    private final double VELOCITY_STEP = 3.0;
    private final double ROTATION_STEP = 4.0;
    private double rotation;
    private double velocity;
    private int selectedAction;
    private boolean emitAction;
    private double power;
    private InputState currentState;

    private double turretPitch;
    private double turretYaw;

    /** _ready
     * runs upon being instantiated in the game world
     * acts as a constructor
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
    }

    /** _process
     * runs every frame to process user input
     */
    @RegisterFunction
    @Override
    public void _process(double delta) {
        Vector2 inputDirection = Input.getVector(
            "backward",
            "forward",
            "right",
            "left"
        );
        if (inputDirection.isZeroApprox()) {
            if (velocity > 0) {
                velocity -= (VELOCITY_STEP / 5) * delta;
            } else if (velocity < 0) {
                velocity += (VELOCITY_STEP / 5) * delta;
            }
        } else {
            rotation += (inputDirection.getY() * ROTATION_STEP * delta) / 3;
            velocity += inputDirection.getX() * VELOCITY_STEP * delta;
            velocity = gd.clamp(velocity, -0.5, 1);
        }

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

    @RegisterFunction
    @Override
    public InputState getState() {
        return currentState;
    }
}

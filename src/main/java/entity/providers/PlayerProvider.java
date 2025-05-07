package entity.providers;

import entity.InputProvider;
import entity.InputState;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.Input;
import godot.core.Vector2;
import godot.global.GD;

@RegisterClass
public class PlayerProvider extends InputProvider {

    private static final GD gd = GD.INSTANCE;

    private final double THROTTLE_STEP = 1.0;
    private final double TURN_STEP = 1.0;
    private double throttle;
    private double angularAcceleration;
    private int selectedAction;
    private boolean emitAction;
    private InputState currentState;

    @RegisterFunction
    @Override
    public void _ready() {
        gd.print("Loaded player provider");
        currentState = new InputState();
        throttle = 0;
        angularAcceleration = 0;
        selectedAction = 0;
        emitAction = false;
    }

    @RegisterFunction
    @Override
    public void _process(double delta) {
        Vector2 inputDirection = Input.getVector(
            "backward",
            "forward",
            "left",
            "right"
        );

        if (!inputDirection.isZeroApprox()) {
            if (inputDirection.getY() == 0) {
                throttle += THROTTLE_STEP * delta * inputDirection.getX();
            } else {
                angularAcceleration = TURN_STEP;
            }
        } else {
            angularAcceleration = 0.0;
        }

        if (Input.isActionJustPressed("performAction")) {
            emitAction = true;
        } else {
            emitAction = false;
        }

        updateState();
    }

    private void updateState() {
        currentState.acceleration = throttle;
        currentState.rotation += angularAcceleration;
        currentState.emitAction = emitAction ? selectedAction : -1;
    }

    @RegisterFunction
    @Override
    public InputState getState() {
        return currentState;
    }
}

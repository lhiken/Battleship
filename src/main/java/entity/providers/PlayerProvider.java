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

    private final double VELOCITY_STEP = 3.0;
    private final double ROTATION_STEP = 4.0;
    private double rotation;
    private double velocity;
    private int selectedAction;
    private boolean emitAction;
    private InputState currentState;

    @RegisterFunction
    @Override
    public void _ready() {
        gd.print("Loaded player provider");
        currentState = new InputState();
        rotation = 0;
        velocity = 0;
        selectedAction = 0;
        emitAction = false;
    }

    @RegisterFunction
    @Override
    public void _process(double delta) {
        Vector2 inputDirection = Input.getVector(
            "backward",
            "forward",
            "right",
            "left"
        );

        rotation += inputDirection.getY() * ROTATION_STEP * delta;
        velocity += inputDirection.getX() * VELOCITY_STEP * delta;
        velocity = gd.clamp(velocity, 0, 1);

        if (Input.isActionJustPressed("performAction")) {
            emitAction = true;
        } else {
            emitAction = false;
        }

        updateState();
    }

    private void updateState() {
        currentState.velocity = velocity;
        currentState.rotation = rotation;
        currentState.emitAction = emitAction ? selectedAction : -1;
    }

    @RegisterFunction
    @Override
    public InputState getState() {
        return currentState;
    }
}

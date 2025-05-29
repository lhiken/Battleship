package entity.providers;

import entity.InputProvider;
import entity.InputState;
import godot.annotation.Export;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.Input;
import godot.core.Vector2;
import godot.global.GD;

/**
 * MultiplayerProvider
 * A version of InputProvider for the local player's input
 */
@RegisterClass
public class MultiplayerProvider extends InputProvider {

    private static final GD gd = GD.INSTANCE;

    @Export int playerID = 1;
    
    private final double VELOCITY_STEP = 3.0;
    private final double ROTATION_STEP = 4.0;
    private double rotation;
    private double velocity;
    private int selectedAction;
    private boolean emitAction;
    private InputState currentState;

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
        selectedAction = 0;
        emitAction = false;
    }

    /**
     * Overrides Godot's internal built-in _process function
     * Runs every frame to process user input
     * @param delta the time elapsed between each call to _process
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

    /**
     * Getter method for it's inputState
     * @return the inputState
     */
    @RegisterFunction
    @Override
    public InputState getState() {
        return currentState;
    }
}

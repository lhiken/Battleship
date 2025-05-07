package entity.providers;

import entity.InputProvider;
import entity.InputState;
import entity.Ship;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.Input;
import godot.core.Vector2;
import godot.global.GD;
import java.util.ArrayList;
import java.util.List;

@RegisterClass
public class BotProvider extends InputProvider {

    private static final GD gd = GD.INSTANCE;

    private final double THROTTLE_STEP = 1.0;
    private final double TURN_STEP = 1.0;
    private double throttle;
    private double angularAcceleration;
    private double radius;
    // private int selectedAction;
    private boolean emitAction;
    private InputState currentState;

    @RegisterFunction
    @Override
    public void _ready() {
        gd.print("Loaded bot provider");
        currentState = new InputState();
        throttle = 0;
        radius = 75;
        angularAcceleration = 0;
        emitAction = false;
    }

    // @RegisterFunction
    // public Vector2 findNearbyEnemies(double radius) {

    //     // How to access locations/input states of other ships?

    // }

    // @RegisterFunction
    // @Override
    // public void _process(double delta) {

    //     if (findNearbyEnemies(radius).isZeroApprox()) {

    //         // Move randomly

    //     }

    //     else {

    //     }

    // }

    // private void updateState() {
    //     currentState.acceleration = throttle;
    //     currentState.rotation += angularAcceleration;
    //     currentState.emitAction = emitAction ? selectedAction : -1;
    // }

    @RegisterFunction
    @Override
    public InputState getState() {
        return currentState;
    }
    //    @RegisterFunction
    //    @Override
    //    public void getState() {
    //
    //    }
}

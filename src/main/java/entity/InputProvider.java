package entity;

import godot.annotation.RegisterClass;
import godot.api.Node;

/** InputProvider
 *
 */
public class InputProvider extends Node {
    private InputState currentState;

    public InputProvider () {
        currentState = new InputState();
    }

    public InputState getState () {
        return currentState;
    }
}
package entity;

import godot.core.Vector2;

/** Input State
 * this class represents an input to the controlled
 * object, for example the ship
 */
public class InputState {
    public Vector2 deltaVelocity;
    public Vector2 deltaAcceleration;
    public Vector2 deltaPosition;
}
package entity;

import godot.core.Vector2;

public class InputState {
    public Double acceleration;
    public Double rotation;
    public Vector2 position;
    public Integer emitAction;

    public InputState () {
        acceleration = 0d;
        rotation = 0d;
        position = null;
        emitAction = 0;
    }

    public double getAcceleration() {
        return acceleration;
    }

    public double getRotation() {
        return rotation;
    }

    public Vector2 getPosition() {
        return position;
    }

    public Integer getEmittedAction() {
        return emitAction;
    }
}
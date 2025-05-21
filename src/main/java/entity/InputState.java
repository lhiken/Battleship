package entity;

import godot.core.Vector2;

public class InputState {

	public Double velocity;
	public Double rotation;
	public Vector2 position;
	public Integer emitAction;
	public Double power;

	public InputState() {
		velocity = 0d;
		rotation = 0d;
		position = null;
		emitAction = 0;
		power = 0d;
	}

	public double getVelocity() {
		return velocity;
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

	public double getPower() { return power; }
}

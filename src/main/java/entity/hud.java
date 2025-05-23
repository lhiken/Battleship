package entity;

import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.CharacterBody3D;

@RegisterClass
public class Weapon extends CharacterBody3D {

	@RegisterFunction
	@Override
	public void _ready() {
		// Initialize logic
	}

	@RegisterFunction
	@Override
	public void _process(double delta) {
		// Per-frame logic
	}

	// Other functions like getCooldown() etc.
}

package entity;

import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.InputEvent;
import godot.api.Node3D;
import godot.global.GD;

@RegisterClass
public class InputProvider extends Node3D {

	private static final GD gd = GD.INSTANCE;

	// input state? (bot or player)

	@RegisterFunction
	@Override
	public void _input(InputEvent event) {
		assert event != null;
		if (event.isActionPressed("w")) {

		}
	}


}

package entity;

import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.InputEvent;
import godot.api.Node3D;
import godot.global.GD;

@RegisterClass
public class InputProvider extends Node3D {
	private static final GD gd = GD.INSTANCE;

	@RegisterFunction
	public InputState getState() {
		gd.print("not implemented grrr");
		return new InputState();
	}

}

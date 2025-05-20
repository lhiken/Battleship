package entity;

import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.Node3D;
import godot.api.RigidBody3D;
import godot.api.Timer;
import godot.core.Vector3;
import godot.global.GD;

@RegisterClass
public class Bullet extends RigidBody3D {

	private static final GD gd = GD.INSTANCE;
	double timeElapsed = 0;

	@RegisterFunction
	@Override
	public void _ready() {

	}

	@RegisterFunction
	@Override
	public void _process(double delta) {

		timeElapsed += delta;

		if (timeElapsed > 5) {
			queueFree();
		}


	}

	@RegisterFunction
	public void shoot(Vector3 rotation, double multiplier) {

		double angle = Math.tan(rotation.getZ()/rotation.getX());
		applyImpulse(new Vector3(Math.sin(rotation.getY()) * 10 * (1 + multiplier/7), 3, Math.cos(rotation.getY()) * 10 * (1 + multiplier/7)));

	}



}

package ui;

import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.CanvasLayer;
import godot.api.Panel;

/**
 * Heads-up display that player sees
 */
@RegisterClass
public class Hud extends CanvasLayer {

	/**
	 * Does nothing
	 */
	@RegisterFunction
	@Override
	public void _ready() {
	}

	/**
	 * Does nothing
	 * @param delta is time passed since last time process is called
	 */
	@RegisterFunction
	@Override
	public void _process(double delta) {
		// Per-frame logic
	}
	// Other functions like getCooldown() etc.
}

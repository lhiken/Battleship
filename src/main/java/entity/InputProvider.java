package entity;

import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.Node3D;
import godot.global.GD;

/**
 * InputProvider is a parent class for it's children classes
 * that include BotProvider (for bots) and PlayerProvider (for human players)
 * <p>
 * InputProvider feeds information about the ship to determine the actions and orientation of the ship
 */
@RegisterClass
public class InputProvider extends Node3D {

	private static final GD gd = GD.INSTANCE;

	/**
	 * Getter method that returns the inputState of a ship
	 * @return the inputState
	 */
	@RegisterFunction
	public InputState getState() {
		gd.print("not implemented grrr"); // :shocked_emoji:
		return new InputState();
	}
}

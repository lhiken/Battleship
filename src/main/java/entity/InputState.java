package entity;

import godot.core.Vector2;

/**
 * The input state of its respective ship.
 * <p>
 * A field of ship's class that contains its ship's
 * velocity, rotation, position, and more that are based on its environment or inputs
 * This class contains getter methods for all of its ship's information
 * <p>
 * The ship simply uses these values to determine its orientation and actions in the world
 */
public class InputState {
	/**
	 * Velocity of the ship calculated by either its path that it needs to take (a bot)
	 * or the w and s keys (a human)
	 */
	public Double velocity;

	/**
	 * The rotation of the ship around the y-axis in radians that is calculated by either
	 * the path a ship needs to take (a bot)
	 * or the a and d keys (a human)
	 */
	public Double rotation;

	/**
	 * The current position of the ship depicted in a Vector2
	 * (x position, z position)
	 * That is calculated by its velocity, rotation, and more
	 */
	public Vector2 position;

	/**
	 * The current action emitted by the ship through
	 * methods such as canShoot() (a bot)
	 * or keyboard inputs (a human)
	 */
	public Integer emitAction;

	/**
	 * The power of a cannonball shot depending on
	 * its turret angles
	 */
	public Double power;

	/**
	 * The rotation of the ship's turret around the y-axis
	 * based on the camera angle (a human)
	 * or aiming code (a bot)
	 */
	public Double turretYaw;

	/**
	 * The rotation of the ship's turret around the x-axis
	 * based on the camera angle (a human)
	 * or the aiming code (a bot)
	 */
	public Double turretPitch;

	/**
	 * Constructor for the inputState
	 */
	public InputState() {
		velocity = 0d;
		rotation = 0d;
		position = null;
		emitAction = -1;
		turretYaw = 0d;
		turretPitch = 0d;
		power = 0d;
	}

	/**
	 * Getter method for the velocity of the ship
	 * @return velocity of the ship
	 */
	public double getVelocity() {
		return velocity;
	}

	/**
	 * Getter method for the rotation of the ship around the y-axis
	 * @return rotation of the ship
	 */
	public double getRotation() {
		return rotation;
	}

	/**
	 * Getter method for the rotation of the ship's turret around the x-axis
	 * @return the pitch
	 */
	public double getPitch() {
		return turretPitch;
	}

	/**
	 * Getter method for the rotation of the ship's turret around the y-axis
	 * @return the yaw
	 */
	public double getYaw() {
		return turretYaw;
	}

	/**
	 * Getter method for the position of the ship disregarding it's y-level (y level never changes)
	 * @return The position in a Vector2 form
	 */
	public Vector2 getPosition() {
		return position;
	}

	/**
	 * Getter method for the current emitted action of the ship
	 * @return the emitted action
	 */
	public Integer getEmittedAction() {
		return emitAction;
	}

	/**
	 * Getter method for the current power of the ship's cannonball shot
	 * @return the power of the cannonball
	 */
	public double getPower() {
		return power;
	}
}

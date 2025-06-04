package entity;

import entity.providers.BotProvider;
import entity.providers.PlayerProvider;
import godot.annotation.Export;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.annotation.RegisterProperty;
import godot.annotation.Rpc;
import godot.annotation.RpcMode;
import godot.annotation.Sync;
import godot.annotation.TransferMode;
import godot.api.*;
import godot.core.NodePath;
import godot.core.StringName;
import godot.core.StringNames;
import godot.core.Vector3;
import godot.global.GD;
import main.Game;
import main.GameCameraFrame;
import main.MatchManager;
import map.gen.Generator;
import multiplayer.MultiplayerManager;

/**
 * Base ship class!!!!
 * <p>
 *     The character that the player (and bots) will be controlling in order to compete in a match
 * </p>
 * <p>
 *     Ship class operates as a dummy class.
 *     This class does not actually calculate any values or changes based on environmental or input factors
 *     The ship class simply takes in values from its inputState and inputProvider in order to determine
 *     its actions and orientation in the world
 * </p>
 * <p>
 *     The ship mainly acts through its process and physicsProcess methods
 *     In each method, the ship accesses its inputState in order to determine movement and actions
 *     The rest of the methods include getter and setter methods to access the ship's private fields
 * </p>
 */
@RegisterClass
public class Ship extends CharacterBody3D {

	private static final GD gd = GD.INSTANCE;
	private double velocity;
	private double rotation;
	private double turretYaw;
	private double turretPitch;
	private InputState state;

	private AudioStreamPlayer boom;
	private AudioStreamPlayer emptyCannon;

	private double maxVelocity = 5.0;

	@Export
	@RegisterProperty
	public Vector3 spawnPosition;

	/**
	 * The respective input provider of each ship
	 */
	@RegisterProperty
	@Export
	public InputProvider provider;

	// pathfinding debug start
	// everything below is useless lmao

	/**
	 * The respective generator of each ship
	 */
	@RegisterProperty
	@Export
	public Generator gen;

	private int frameCounter = 0;

	private double cooldownTime = 2;
	private double cooldownPercent = 1;

	private double health = 100;

	// private MeshInstance3D trajectoryMesh;

	private boolean sinking = false;

	/**
	 * Sets up multiplayer id and gives ship full health when program runs
	 */
	@RegisterFunction
	@Override
	public void _ready() {
		setMultiplayerAuthority(1);

		setGlobalPosition(spawnPosition);

		health = 100;
		// instantiateNewCannon();
	}

	/**
	 * Sets up the spawn position for this ship instance
	 */
	@RegisterFunction
	public void setSpawn(Vector3 spawn) {
		this.spawnPosition = spawn;
	}

	/**
	 * Runs every single frame and provided by Godot
	 *
	 * Uses inputState and inputProvider in order to calculate each ships respective velocity, turret rotation, shooting, etc
	 * in order to calculate its current orientation and if its shooting
	 *
	 * @param delta is the time passed from the last process being called
	 */
	@RegisterFunction
	@Override
	public void _process(double delta) {
		if (
			!getName().toString().startsWith("Bot") &&
			!getName().toString().startsWith("S")
		) {
			setMultiplayerAuthority(Integer.parseInt(getName().toString()));
		}
		frameCounter++;

		cooldownPercent += delta / cooldownTime;
		cooldownPercent = gd.clamp(cooldownPercent, 0, 1);

		if (health <= 0 && !sinking) {
			if (
				getName().toString().startsWith("Bot") &&
				MultiplayerManager.Instance.isServer()
			) {
				gd.print("instantiating new bot");
				((MatchManager) getParent().getParent()).instantiateNewBot();
			}
			getNode("ShipMesh/Sails").queueFree();
			setName("S" + Math.random() * 100000 + getName());
			((CollisionShape3D) getNode("ShipCollider")).setDisabled(true); // i
			((CollisionShape3D) getNode("ShipCollider2")).setDisabled(true); // know
			((CollisionShape3D) getNode("ShipCollider3")).setDisabled(true); // but!
			((Label3D) getNode("NameTag")).setVisible(false);
			sinking = true;
		}

		if (sinking) {
			globalTranslate(
				new Vector3(
					0,
					(getGlobalPosition().getY() * 0.1 - 0.5) * delta * 0.25,
					0
				)
			);

			velocity = gd.lerp(velocity, 0, 0.02);

			if (getGlobalPosition().getY() < -15) {
				queueFree();
			}

			sinking = true;

			return;
		}

		if (health < 100) health += delta * 1;

		if (state == null) return;

		turretYaw = gd.lerpAngle(turretYaw, state.getYaw(), 0.1);
		turretPitch = gd.lerpAngle(turretPitch, state.getPitch(), 0.1);

		Vector3 position = this.getGlobalPosition();
		position.setY(position.getY() + 3);

		if (provider != null) {
			state = provider.getState();

			if (state.getEmittedAction() != -1 && cooldownPercent == 1) {
				MatchManager matchManager = (MatchManager) getParent()
					.getParent();
				Vector3 origin =
					((Node3D) getNode(
							"Turret/Cannon/ProjectileOrigin"
						)).getGlobalPosition();

				int id = getMultiplayer().getUniqueId();

				if (
					getName().toString().length() > 4 &&
					getName().toString().startsWith("Bot")
				) id = Integer.parseInt(getName().toString().substring(3)) * -1;

				matchManager.rpc(
					StringNames.toGodotName("spawnBullet"),
					id,
					new Vector3(
						Math.sin(state.getYaw()),
						Math.sin(state.getPitch()),
						Math.cos(state.getYaw())
					).normalized(),
					origin,
					getVelocity()
				);
				// gd.print(state.getPower());
				// cannon.instantiateNewBullet(
				//     this.getRotation(),
				//     velocity + state.getPower()
				// );

				// uncommenting this before fixing it will cause process to terminate early
				// resulting in cooldown never being reset and firing forever!!
				cooldownPercent = 0;
				boom = (AudioStreamPlayer) getNode("CannonFire");
				// if (Integer.parseInt((this).getName().toString()) != this.getMultiplayer().getUniqueId()) boom.setVolumeDb(-10);
				if (provider instanceof PlayerProvider && this.getMultiplayer().getUniqueId() == Integer.parseInt((this).getName().toString())) boom.play();
			} else if (
				state.getEmittedAction() != -1 &&
				cooldownPercent < 1 &&
				provider instanceof PlayerProvider
			) {
				if (this.getMultiplayer().getUniqueId() == Integer.parseInt((this).getName().toString())) {
					emptyCannon = (AudioStreamPlayer) getNode("EmptyCannon");
					emptyCannon.play();
				}
			}
		}
		// drawProjectilePath();
	}

	/**
	 * Setter method that sets health of ship
	 * @param health health of the ship
	 */
	@Rpc(
		rpcMode = RpcMode.ANY,
		sync = Sync.NO_SYNC,
		transferMode = TransferMode.RELIABLE
	)
	@RegisterFunction
	public void setHealth(double health) {
		this.health = health;
		if (getMultiplayer().isServer()) rpc(
			StringNames.toGodotName("setHealth"),
			health
		);
	}

	/**
	 * Getter method that gets the health of ship
	 * @return health of ship
	 */
	@RegisterFunction
	public double getHealth() {
		return health;
	}

	/**
	 * Getter method for ship's pitch
	 * @return the rotation of the ship's turret around the x-axis
	 */
	@RegisterFunction
	public double getPitch() {
		return turretPitch;
	}

	/**
	 * Getter method for the ship's yaw
	 * @return the rotation of the ship's turret around the y-axis
	 */
	@RegisterFunction
	public double getYaw() {
		return turretYaw;
	}

	/**
	 * Getter method for inputState
	 * @return the input state of the ship
	 */
	public InputState getState() {
		return provider.getState();
	}

	/**
	 * Overriding Godot's built-in function
	 *
	 * "processes" differently than Godot's _process method,
	 * more accurate for physics
	 *
	 * handles movement, orientation, sinking and rocking motions based on ship's inputState and inputProvider
	 * @param delta the time passed between each call to physicsProcess
	 */
	@RegisterFunction
	@Override
	public void _physicsProcess(double delta) {
		if (sinking) {
			// still apply forward movement (but fading out)
			Vector3 direction = new Vector3(
				Math.sin(rotation),
				0,
				Math.cos(rotation)
			);
			setVelocity(direction.times(velocity));
			moveAndSlide();
			return;
		}

		if (isMultiplayerAuthority() && !sinking) {
			if (provider == null) return;

			state = provider.getState();

			double targetVelocity = state.getVelocity() * maxVelocity;
			double targetRotation = state.getRotation();

			velocity = gd.lerp(velocity, targetVelocity, 0.1);
			rotation = gd.lerpAngle(rotation, targetRotation, 0.1);

			Vector3 direction = new Vector3(
				Math.sin(rotation),
				0,
				Math.cos(rotation)
			);

			double time = frameCounter / 40.0;
			double rockX = Math.sin(time * 0.7) * 0.025;
			double rockZ = Math.sin(time * 0.9) * 0.015;
			Vector3 rockingOffset = new Vector3(rockX, 0, rockZ);

			setPosition(
				new Vector3(getPosition().getX(), 0, getPosition().getZ())
			);
			setVelocity(direction.times(velocity));
			setRotation(new Vector3(0, rotation, 0).plus(rockingOffset));
		}
		moveAndSlide();
	}

	/**
	 * Setter method for the ship's InputProvider
	 * @param provider the provider to be set to this ship's provider field
	 */
	@RegisterFunction
	public void setProvider(InputProvider provider) {
		this.provider = provider;
	}

	/**
	 * Getter method for the ship's cooldown percentage
	 * @return the ship's cooldown percentage
	 */
	@RegisterFunction
	public double getCooldownPercentage() {
		return cooldownPercent;
	}

	/**
	 * Getter method for if the ship is sinking
	 * @return boolean depicting ships sinking state
	 */
	@RegisterFunction
	public boolean isSinking() {
		return sinking;
	}

	@RegisterFunction
	public InputProvider getProvider() { return provider; }
}

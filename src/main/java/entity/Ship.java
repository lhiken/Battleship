package entity;

import godot.annotation.Export;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.annotation.RegisterProperty;
import godot.api.*;
import godot.core.Color;
import godot.core.NodePath;
import godot.core.Vector2;
import godot.core.Vector3;
import godot.global.GD;
import java.util.ArrayList;
import map.gen.Coordinate;
import map.gen.Generator;

/** Ship
 * base ship class nothing works rn please fix :pray:
 */
@RegisterClass
public class Ship extends CharacterBody3D {

	private static final GD gd = GD.INSTANCE;
	private double velocity;
	private double rotation;
	private InputState state;
	private Weapon cannon;
	private AudioStreamPlayer3D boom;

	private double maxVelocity = 5.0;

	@RegisterProperty
	@Export
	public InputProvider provider;

	// pathfinding debug start
	// everything below is useless lmao

	@RegisterProperty
	@Export
	public Generator gen;

	private int frameCounter = 0;

	@RegisterFunction
	@Override
	public void _ready() {
		setMultiplayerAuthority(Integer.parseInt(getName().toString()));
		// instantiateNewCannon();
		// boom = (AudioStreamPlayer3D) getNode(new NodePath("CannonFire"));
	}

	@RegisterFunction
	@Override
	public void _process(double delta) {
		frameCounter++;

		if (!isMultiplayerAuthority()) {
			return;
		}
		// frameCounter++;
		// if (frameCounter % 60 != 0) {
		//     return;
		// }

		// Vector3 currentPosition = new Vector3(
		//     getPosition().getX(),
		//     0,
		//     getPosition().getZ()
		// );
		// Vector3 endPosition = new Vector3(0, 0, 0);
		// ArrayList<Coordinate> path = gen.navigate(currentPosition, endPosition);
		// clearPathVisualization();

		// for (Coordinate coord : path) {
		//     CSGSphere3D sphere = new CSGSphere3D();
		//     sphere.setRadius(0.2f);

		//     Vector3 position = coord.toVec3();
		//     sphere.setPosition(position);

		//     StandardMaterial3D material = new StandardMaterial3D();
		//     material.setAlbedo(new Color(0, 1, 0, 0.8f));
		//     sphere.setMaterial(material);
		//     getParent().addChild(sphere);

		//     pathVisualization.add(sphere);
		// }

		Vector3 position = this.getGlobalPosition();
		position.setY(position.getY() + 3);
		// cannon.setPosition(position);

		if (provider != null) {
			state = provider.getState();

			if (state.getEmittedAction() != -1) {
				// //				gd.print("pew pew");

				// gd.print(state.getPower());
				// cannon.instantiateNewBullet(
				//     this.getRotation(),
				//     velocity + state.getPower()
				// );
				// if (boom != null) {
				//     gd.print(boom);
				//     boom.play();
				//     //					gd.print("Ship pos: " + this.getGlobalPosition());
				//     //					gd.print("Sound pos: " + boom.getGlobalPosition());
				// } else {
				//     gd.print("you wish");
				// }
			}
		}
	}

	@RegisterFunction
	public double getPitch() {
		gd.print("pitch: " + gd.radToDeg(state.getPitch()));
		return state.getPitch();
	}

	@RegisterFunction
	public double getYaw() {
		gd.print("yaw: " + gd.radToDeg(state.getYaw()));
		return state.getYaw();
	}

	// private void instantiateNewCannon() {
	//     PackedScene weapon = gd.load("res://components/objects/weapon.tscn");
	//     cannon = (Weapon) (weapon.instantiate());

	//     Vector3 pos = this.getGlobalPosition();
	//     pos.setY(pos.getY() + 3);
	//     cannon.setPosition(pos);
	//     cannon.setRotation(this.getRotation());

	//     getParent().addChild(cannon);
	// }

	public InputState getState() {
		return provider.getState();
	}

	private ArrayList<Node> pathVisualization = new ArrayList<>();

	private void clearPathVisualization() {
		for (Node node : pathVisualization) {
			removeChild(node);
			node.queueFree();
		}
		pathVisualization.clear();
	}

	// pathfinding debug end

	@RegisterFunction
	@Override
	public void _physicsProcess(double delta) {
		if (isMultiplayerAuthority()) {
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

			setVelocity(direction.times(velocity));
			setRotation(new Vector3(0, rotation, 0).plus(rockingOffset));
		}
		moveAndSlide();
	}

	@RegisterFunction
	public void setProvider(InputProvider provider) {
		this.provider = provider;
	}
}

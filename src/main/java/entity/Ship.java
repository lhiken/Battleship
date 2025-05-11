package entity;

import godot.annotation.Export;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.annotation.RegisterProperty;
import godot.api.CSGSphere3D;
import godot.api.CharacterBody3D;
import godot.api.Node;
import godot.api.StandardMaterial3D;
import godot.core.Color;
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

    private double maxVelocity = 10.0;

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
    public void _process(double delta) {
        frameCounter++;
        if (frameCounter % 60 != 0) {
            return;
        }

        Vector2 currentPosition = new Vector2(
            getPosition().getX(),
            getPosition().getZ()
        );
        Vector2 endPosition = new Vector2(0, 0);
        ArrayList<Coordinate> path = gen.navigate(currentPosition, endPosition);
        clearPathVisualization();

        for (Coordinate coord : path) {
            CSGSphere3D sphere = new CSGSphere3D();
            sphere.setRadius(0.2f);

            Vector3 position = coord.toVec3();
            sphere.setPosition(position);

            StandardMaterial3D material = new StandardMaterial3D();
            material.setAlbedo(new Color(0, 1, 0, 0.8f));
            sphere.setMaterial(material);
            getParent().addChild(sphere);

            pathVisualization.add(sphere);
        }
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
        if (provider == null) return;

        state = provider.getState();

        double targetVelocity = state.getVelocity() * maxVelocity;
        double targetRotation = state.getRotation();

        velocity = gd.lerp(velocity, targetVelocity, 0.1);
        rotation = gd.lerpAngle(rotation, targetRotation, 0.1);

        // Rotate the forward direction by the Y rotation
        Vector3 direction = new Vector3(
            Math.sin(rotation),
            0,
            Math.cos(rotation)
        );

        setVelocity(direction.times(velocity));
        setRotation(new Vector3(0, rotation, 0));

        moveAndSlide();
    }
}

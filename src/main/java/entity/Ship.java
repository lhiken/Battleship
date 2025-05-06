package entity;

import godot.annotation.Export;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.annotation.RegisterProperty;
import godot.api.InputEvent;
import godot.api.Node;
import godot.api.Node3D;
import godot.api.CharacterBody3D;
import godot.core.Vector2;
import godot.core.Vector3;
import godot.global.GD;


/** Ship
 * base ship class nothing works rn please fix :pray:
 */
@RegisterClass
public class Ship extends CharacterBody3D {

    private static final GD gd = GD.INSTANCE;
    private double forwardVelocity;
    private double angularVelocity;
    private InputState state;

    private double maxForwardVelocity = 1.0;
    private double maxAngularVelocity = 1.0;

    @RegisterProperty
    @Export
    public InputProvider provider;

//    @RegisterFunction
//    @Override
//    public void _ready() {
//        provider = (InputProvider) getParent();
//    }

    @RegisterFunction
    @Override
    public void _physicsProcess(double delta) {
        state = provider.getState();

        forwardVelocity += state.getAcceleration() * delta;
        angularVelocity += state.getRotation() * delta;

        int fvOffset = forwardVelocity > 0 ? 1 : -1;
        int avOffset = angularVelocity > 0 ? 1 : -1;

        forwardVelocity = Math.min(maxForwardVelocity, Math.abs(forwardVelocity)) * fvOffset;
        angularVelocity = Math.min(maxAngularVelocity, Math.abs(angularVelocity)) * avOffset;

        // testing
        gd.print(forwardVelocity);
        gd.print(angularVelocity);

        Vector3 direction = getVelocity().normalized();
        translate(direction.times(forwardVelocity));
        rotate(new Vector3(0, 1, 0), (float) (angularVelocity * delta));

        moveAndSlide();
    }

    private Vector3 vec2ToVec3(Vector2 vec2, char axis, float defVal) {
        Vector3 newVec = new Vector3();
        if (axis == 'X') {
            newVec.setX(defVal);
            newVec.setY(vec2.getX());
            newVec.setZ(vec2.getY());
        } else if (axis == 'Y') {
            newVec.setY(defVal);
            newVec.setX(vec2.getX());
            newVec.setZ(vec2.getY());
        } else if (axis == 'Z') {
            newVec.setZ(defVal);
            newVec.setX(vec2.getX());
            newVec.setY(vec2.getY());
        }
        return newVec;
    }
}

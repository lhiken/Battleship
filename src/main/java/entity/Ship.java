package entity;

import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.annotation.RegisterProperty;
import godot.api.InputEvent;
import godot.api.Node3D;
import godot.api.RigidBody3D;
import godot.global.GD;

@RegisterClass
public class Ship extends RigidBody3D {

    private static final GD gd = GD.INSTANCE;

    @RegisterProperty
    private Node3D input = new InputProvider();

    @RegisterFunction
    @Override
    public void _process(double delta) {


    }

}

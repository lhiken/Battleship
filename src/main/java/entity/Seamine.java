package entity;

import godot.annotation.Export;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterProperty;
import godot.api.MeshInstance3D;
import godot.api.PackedScene;

@RegisterClass
public class Seamine extends MeshInstance3D {

    @RegisterProperty
    @Export
    public PackedScene explosion;

}

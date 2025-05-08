package main;

import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.Node3D;
import godot.global.GD;

/** Game
 * the class for the main scene
 */
@RegisterClass
public class Game extends Node3D {

    // include this line everywhere bc godot's methods use it
    private static final GD gd = GD.INSTANCE;

    // ready runs when this object is loaded
    @RegisterFunction
    @Override
    public void _ready() {
        gd.print("loaded game");
    }

    @RegisterFunction
    public void initializeHost() {
        gd.print("testing testing lmao");
    }

    @RegisterFunction
    public void initializeClient() {}
}

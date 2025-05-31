package main;

import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.MultiplayerAPI;
import godot.api.Node;
import godot.api.Node3D;
import godot.api.PackedScene;
import godot.core.Callable;
import godot.core.StringName;
import godot.core.StringNames;
import godot.global.GD;
import java.util.ArrayList;
import java.util.HashMap;
import multiplayer.MultiplayerManager;

/**
 * The class for the main scene of the game, which
 * includes a method to start the game
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

        gd.print("preloading resources");

        PackedScene bullet = gd.load("res://components/objects/bullet.tscn");
        Node3D bulletNode = (Node3D) bullet.instantiate();
        addChild(bulletNode);
        bulletNode.queueFree();

        MultiplayerManager.Instance.multiplayerConnected.connect(
            Callable.create(this, StringNames.toGodotName("spawnMatch")),
            0
        );
    }

    @RegisterFunction
    public void spawnMatch(boolean success) {
        if (getNode("Match") != null || !success) return;
        PackedScene matchScene = gd.load("res://components/main/match.tscn");
        getNode("StartMenu").queueFree();
        getNode("RenderTarget").queueFree();
        getNode("Props").queueFree();
        Node matchNode = matchScene.instantiate();
        addChild(matchNode);
    }
}

package entity;

import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.Label;
import godot.api.Label3D;
import godot.global.GD;
import multiplayer.MultiplayerManager;

@RegisterClass
public class NameTag extends Label3D {

    private GD gd = GD.INSTANCE;

    @RegisterFunction
    @Override
    public void _ready() {
        MultiplayerManager manager = MultiplayerManager.Instance;
        String name = getParent().getName().toString();
        if (name.startsWith("Bot")) {
            setText(name);
        } else {
            setText(
                manager.getPlayerData(Integer.parseInt(name)).getUsername()
            );
        }
    }
}

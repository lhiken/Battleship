package entity;

import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.Label;
import godot.global.GD;
import multiplayer.MultiplayerManager;

@RegisterClass
public class NameTag extends Label {
    private GD gd = GD.INSTANCE;

    @RegisterFunction
    @Override
    public void _ready() {
        MultiplayerManager manager = MultiplayerManager.Instance;
        String name = getParent().getName().toString();
        //gd.print("wahoo");
        if (name.startsWith("Bot")) {
            setText(name);
        } else {
            setText(manager.getPlayerData(Integer.parseInt(name)).getUsername());
            //gd.print("wabooooo");
        }
    }
}

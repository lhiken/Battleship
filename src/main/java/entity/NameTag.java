package entity;

import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.Label;
import godot.api.Label3D;
import godot.global.GD;
import multiplayer.MultiplayerManager;

/**
 * The name tag of each ship
 */
@RegisterClass
public class NameTag extends Label3D {

    private GD gd = GD.INSTANCE;

    /**
     * When game starts, finds randomly generated name
     * of each ship and sets text of name tag's label to the name
     */
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

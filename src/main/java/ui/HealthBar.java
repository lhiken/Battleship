package ui;

import entity.Ship;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.Node;
import godot.api.TextureProgressBar;
import godot.core.VariantArray;
import godot.global.GD;
import multiplayer.MultiplayerManager;

@RegisterClass
public class HealthBar extends TextureProgressBar {

    private static final GD gd = GD.INSTANCE;
    private double health = 0;

    VariantArray<Node> ships;

    // finding our own ship
    Ship ourShip;

    MultiplayerManager manager = MultiplayerManager.Instance;

    @RegisterFunction
    @Override
    public void _process(double delta) {
        if (ourShip == null) {
            ships = getParent().getParent().getNode("Ships").getChildren();
            for (Node ship : ships) {
                Ship temp = (Ship) ship;
                if (
                    temp.getName().toString().equals(manager.getPeerId() + "")
                ) {
                    ourShip = (Ship) ship;
                }
            }
        }
        if (ourShip != null) {
            health = gd.lerp(health, ourShip.getHealth(), 0.1);
            setValue(health);
            if (ourShip.isSinking()) ourShip = null;
        }
    }
}

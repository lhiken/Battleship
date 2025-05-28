package ui;

import entity.Ship;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.TextureProgressBar;
import godot.core.KtObject;
import godot.core.VariantArray;
import godot.global.GD;
import multiplayer.MultiplayerManager;
import godot.api.Node;

import java.util.ArrayList;

@RegisterClass
public class HealthBar extends TextureProgressBar {

    private static final GD gd = GD.INSTANCE;

    VariantArray<Node> ships;

    // finding our own ship
    Ship ourShip;
    double health;

    MultiplayerManager manager = MultiplayerManager.Instance;

    @RegisterFunction
    @Override
    public void _ready() {
        gd.print("Hello");
        health = ourShip.getHealth();
    }

    @RegisterFunction
    @Override
    public void _process(double delta) {
        if (ourShip == null) {
            ships = getParent().getParent().getNode("Ships").getChildren();
            gd.print("wahoo!!!");
            for (Node ship : ships) {
                Ship temp = (Ship) ship;
                if (temp.getName().toString().equals(manager.getPeerId() + "")) {
                    ourShip = (Ship) ship;
                }
            }
        }

        gd.print(ourShip.getHealth());

        setValue(ourShip.getHealth());
    }

}

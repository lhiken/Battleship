package ui;

import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.CanvasLayer;
import godot.api.Label;
import godot.api.Panel;
import godot.global.GD;
import multiplayer.MultiplayerManager;

/**
 * Heads-up display that player sees
 */
@RegisterClass
public class Hud extends CanvasLayer {

    private GD gd = GD.INSTANCE;
    private double realPoints = 0;

    /**
     * Hides HUD at the start of the game
     */
    @RegisterFunction
    @Override
    public void _ready() {
        this.setVisible(false);
    }

    /**
     * Does nothing
     * @param delta is time passed since last time process is called
     */
    @RegisterFunction
    @Override
    public void _process(double delta) {
        if (
            MultiplayerManager.Instance.getPlayerData(
                getMultiplayer().getUniqueId()
            ) ==
            null
        ) return;
        realPoints = gd.lerp(
            (double) realPoints,
            (double) MultiplayerManager.Instance.getPlayerData(
                getMultiplayer().getUniqueId()
            ).getPoints(),
            2.0 * delta
        );
        ((Label) getNode("Points")).setText(Math.round(realPoints) + " Pts");
    }
    // Other functions like getCooldown() etc.
}

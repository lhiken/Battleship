package ui;

import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.CanvasLayer;
import godot.api.Panel;

@RegisterClass
public class Hud extends CanvasLayer {
    @RegisterFunction
    @Override
    public void _ready() {
        // Initialize logic
    }

    @RegisterFunction
    @Override
    public void _process(double delta) {
        // Per-frame logic
    }
    // Other functions like getCooldown() etc.
}

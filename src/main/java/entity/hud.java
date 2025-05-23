package entity;

import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.CharacterBody3D;
import godot.api.Panel;

@RegisterClass
public class Hud extends Panel {

    @RegisterFunction
    @Override
    public void _ready() {
        // Initialize logic
    }

    @RegisterFunction
    @Override
    public void _process(double dqelta) {
        // Per-frame logic
    }
    // Other functions like getCooldown() etc.
}

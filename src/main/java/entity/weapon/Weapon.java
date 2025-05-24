package entity.weapon;

import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.*;
import godot.core.Vector3;
import godot.global.GD;

@RegisterClass
public class Weapon extends CharacterBody3D {

    private static final GD gd = GD.INSTANCE;
    Vector3 angle; // pass this into instantiateNewBullet instead
    double cooldown = 0;

    @RegisterFunction
    @Override
    public void _ready() {}

    @RegisterFunction
    @Override
    public void _process(double delta) {
        cooldown += delta;
    }

    public void instantiateNewBullet(Vector3 rotation, double multiplier) {
        // if (cooldown > 3) {
        //     PackedScene bullet = gd.load(
        //         "res://components/objects/bullet.tscn"
        //     );
        //     Bullet cannonball = (Bullet) (bullet.instantiate());

        //     Vector3 pos = this.getGlobalPosition();
        //     pos.setY(pos.getY() + 1);
        //     cannonball.setPosition(pos);
        //     //		cannonball.setRotation(this.getRotation());
        //     //		gd.print(this.getRotation().getX() + " " + this.getRotation().getY() + " " + this.getRotation().getZ());
        //     // cannonball.shoot(rotation, Math.abs(multiplier));

        //     getParent().getParent().addChild(cannonball);

        //     cooldown = 0;
        // }
    }

    public double getCooldown() {
        return Math.min(cooldown / 3.0, 1.0);
    }
}

package main;

import entity.Ship;
import godot.annotation.RegisterFunction;
import godot.annotation.RegisterProperty;
import godot.api.Node;

public class MatchManager extends Node {

    @RegisterProperty
    Ship[] ships = new Ship[10];

    @RegisterFunction
    @Override
    public void _process(double delta) {

        for (Ship ship : ships) {
            // idfk how process works like WHO is calling all these process functions :pray: are they all just collectively being processed at once
        }
    }


}
package main;

import entity.Ship;
import entity.providers.PlayerProvider;
import godot.annotation.Export;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.annotation.RegisterProperty;
import godot.annotation.Rpc;
import godot.annotation.RpcMode;
import godot.annotation.Sync;
import godot.api.Control;
import godot.api.MultiplayerAPI;
import godot.api.Node;
import godot.api.Node3D;
import godot.api.PackedScene;
import godot.api.RigidBody3D;
import godot.core.Signal1;
import godot.core.StringName;
import godot.core.StringNames;
import godot.core.Vector3;
import godot.global.GD;
import java.util.ArrayList;
import multiplayer.MultiplayerManager;
import multiplayer.PlayerData;
import ui.lobby.Lobby;

@RegisterClass
public class MatchManager extends Node {

    private GD gd = GD.INSTANCE;

    private Vector3[] spawnLocations;

    @Export
    @RegisterProperty
    public GameCamera gameCamera;

    @RegisterFunction
    @Override
    public void _ready() {
        gd.print("loaded match manager");
        gameCamera.setSpectatorMode();
        // MultiplayerAPI multiplayer = getMultiplayer();
        // var peerIds = multiplayer.getPeers();

        // for (long peerId : peerIds) {
        //     instantiateNewPlayer(peerId);
        // }
    }

    public Ship instantiateNewPlayer(int playerId) {
        PackedScene ship = gd.load("res://components/ships/pirate_ship.tscn");
        PackedScene playerProvider = gd.load(
            "res://components/providers/player_provider.tscn"
        );

        Ship shipNode = (Ship) (ship.instantiate());
        PlayerProvider provider =
            (PlayerProvider) (playerProvider.instantiate());

        shipNode.addChild(provider);
        shipNode.setName(playerId + "");
        shipNode.setProvider(provider);
        shipNode.translate(
            new Vector3(Math.random() * 10.0, 0, Math.random() * 10.0)
        );

        getNode("Ships").addChild(shipNode);

        return shipNode;
    }

    @Rpc
    @RegisterFunction
    public void startMatch() {
        if (MultiplayerManager.Instance.isServer()) {
            ArrayList<PlayerData> players =
                MultiplayerManager.Instance.getSortedPlayerList();

            for (PlayerData player : players) {
                instantiateNewPlayer(player.getPeerId());
            }

            rpc(StringNames.toGodotName("startMatch"));
        }

        Ship playerShip = (Ship) getNode(
            "Ships/" + getMultiplayer().getUniqueId()
        );

        gameCamera.setShip(playerShip);
        gameCamera.setPlayerMode();

        ((Lobby) getNode("Lobby")).setVisible(false);
    }

    @Rpc(rpcMode = RpcMode.ANY, sync = Sync.SYNC)
    @RegisterFunction
    public void spawnBullet(
        int peerOrigin,
        Vector3 direction,
        Vector3 position,
        Vector3 shipVelocity
    ) {
        PackedScene bullet = gd.load("res://components/objects/bullet.tscn");
        RigidBody3D bulletNode = (RigidBody3D) bullet.instantiate();
        bulletNode.setLinearVelocity(shipVelocity);
        bulletNode.setGlobalPosition(position);
        getNode("Bullets").addChild(bulletNode);
        bulletNode.applyImpulse(direction.times(25.0));
    }
}

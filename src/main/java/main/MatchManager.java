package main;

import entity.Ship;
import entity.providers.BotProvider;
import entity.providers.PlayerProvider;
import entity.weapon.Bullet;
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
import map.gen.Generator;
import multiplayer.MultiplayerManager;
import multiplayer.PlayerData;
import ui.lobby.Lobby;

/**
 * The MatchManager class
 * The match scene
 * <p>
 * Manages its players and their actions
 * <p>
 * Contains methods to instantiate either a bot or a player ship
 * Contains method to start match and create a new bullet
 * And various other miscellaneous methods
 */
@RegisterClass
public class MatchManager extends Node {

    private GD gd = GD.INSTANCE;

    private Vector3[] spawnLocations;

    private int botId = 0;

    /**
     * Reference to the gameCamera that is displayed
     */
    @Export
    @RegisterProperty
    public GameCamera gameCamera;

    /**
     * Overrides Godot's built-in _ready function
     * Acts as a constructor
     * Instantiates bots and whatever is needed to make MatchManager function properly
     */
    @RegisterFunction
    @Override
    public void _ready() {
        gd.print("loaded match manager");
        gameCamera.setSpectatorMode();

        MultiplayerManager manager = MultiplayerManager.Instance;
        if (!manager.isServer()) return;

        instantiateNewBot();
        instantiateNewBot();
        // MultiplayerAPI multiplayer = getMultiplayer();
        // var peerIds = multiplayer.getPeers();

        // for (long peerId : peerIds) {
        //     instantiateNewPlayer(peerId);
        // }
    }

    /**
     * Instantiates a new human player ship
     * @param playerId The playerId for the ship to be instantiated
     * @return the Ship that was instantiated
     */
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

        getNode("Ships").addChild(shipNode, true);

        return shipNode;
    }

    /**
     * Instantiates a new bot player ship
     * Does not require an id
     * @return the Ship that was instantiated
     */
    public Ship instantiateNewBot() {
        PackedScene ship = gd.load("res://components/ships/pirate_ship.tscn");
        PackedScene botProvider = gd.load(
            "res://components/providers/bot_provider.tscn"
        );

        Ship shipNode = (Ship) (ship.instantiate());
        BotProvider provider = (BotProvider) (botProvider.instantiate());

        provider.setGenerator((Generator) getNode("Generator"));

        shipNode.addChild(provider);
        shipNode.setName("Bot" + (1000 + botId));
        shipNode.setProvider(provider);
        shipNode.translate(
            new Vector3(Math.random() * 10.0, 0, Math.random() * 10.0)
        );

        getNode("Ships").addChild(shipNode);

        botId++;

        return shipNode;
    }

    /**
     * At the start of a match, creates
     * A collection of both bot and human player ships
     * Sets the camera and playerShip
     * And takes the player from the lobby to the actual match
     */
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

    /**
     * Spawns a bullet and moves it in a certain direction depending on
     * Its direction, position, and ship velocity
     * @param peerOrigin The unique peerId of a ship
     * @param direction The direction that the bullet was shot in (yaw)
     * @param position The position at which it was shot from
     * @param shipVelocity The ship velocity when the bullet was shot
     */
    @Rpc(rpcMode = RpcMode.ANY, sync = Sync.SYNC)
    @RegisterFunction
    public void spawnBullet(
        int peerOrigin,
        Vector3 direction,
        Vector3 position,
        Vector3 shipVelocity
    ) {
        PackedScene bullet = gd.load("res://components/objects/bullet.tscn");
        Bullet bulletNode = (Bullet) bullet.instantiate();
        bulletNode.setOwner(peerOrigin);
        bulletNode.setLinearVelocity(shipVelocity);
        bulletNode.setPosition(position);
        getNode("Bullets").addChild(bulletNode, true);
        bulletNode.applyImpulse(direction.times(25.0));
    }
}

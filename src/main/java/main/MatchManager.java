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
import godot.annotation.TransferMode;
import godot.api.*;
import godot.api.Button;
import godot.api.Control;
import godot.api.MultiplayerAPI;
import godot.api.Node;
import godot.api.Node3D;
import godot.api.PackedScene;
import godot.api.RigidBody3D;
import godot.core.Signal1;
import godot.core.StringName;
import godot.core.StringNames;
import godot.core.Vector2;
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
    private ArrayList<Vector2> spawnLocations = new ArrayList<>();
    private int botId = 0;
    private int spawnIndex = 0;

    private boolean isStarted = false;

    CanvasLayer hud;

    /**
     * Reference to the gameCamera that is displayed
     */
    @Export
    @RegisterProperty
    public GameCamera gameCamera;

    private boolean gameStarted = false;

    public MatchManager() {
        int spawns = 6;
        int spawnRadius = 50;
        double angle = 0;
        for (int i = 0; i < spawns; i++) {
            Vector2 location = new Vector2(
                Math.cos(angle) * spawnRadius,
                Math.sin(angle) * spawnRadius
            );
            spawnLocations.add(location);

            angle += (2 * Math.PI) / spawns;
        }
    }

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

        for (Vector2 position : spawnLocations) {
            PackedScene node = gd.load(
                "res://components/zones/spawn_zone.tscn"
            );
            Node3D spawnComponent = (Node3D) node.instantiate();
            Vector3 pos = new Vector3(position.getX(), 0, position.getY());
            spawnComponent.setGlobalPosition(pos);
            addChild(spawnComponent);
        }

        if (!manager.isServer()) return;
    }

    /**
     * Instantiates a new human player ship
     * @param playerId The playerId for the ship to be instantiated
     * @return the Ship that was instantiated
     */
    @Rpc(rpcMode = RpcMode.ANY, sync = Sync.SYNC)
    @RegisterFunction
    public Ship instantiateNewPlayer(int playerId) {
        gd.print(MultiplayerManager.Instance.getPeerId() + "received rpc");
        PackedScene ship = gd.load("res://components/ships/pirate_ship.tscn");
        PackedScene playerProvider = gd.load(
            "res://components/providers/player_provider.tscn"
        );

        Ship shipNode = (Ship) (ship.instantiate());
        PlayerProvider provider =
            (PlayerProvider) (playerProvider.instantiate());

        Vector2 spawn = spawnLocations.get(spawnIndex % spawnLocations.size());
        gd.print(spawn);

        shipNode.addChild(provider);
        shipNode.setProvider(provider);
        shipNode.spawnPosition = new Vector3(spawn.getX(), 0, spawn.getY());
        shipNode.setName(playerId + "");
        shipNode.setGlobalPosition(new Vector3(spawn.getX(), 0, spawn.getY()));

        getNode("Ships").addChild(shipNode, true);

        spawnIndex++;

        rpcId(playerId, StringNames.toGodotName("setupCamera"));
        gd.print(MultiplayerManager.Instance.getPeerId() + "called camera rpc");

        return shipNode;
    }

    /**
     * Instantiates a new bot player ship
     * Does not require an id
     * @return the Ship that was instantiated
     */
    public Ship instantiateNewBot() {
        gd.print("actually instantiating new bot");

        PackedScene ship = gd.load("res://components/ships/pirate_ship.tscn");
        PackedScene botProvider = gd.load(
            "res://components/providers/bot_provider.tscn"
        );

        Ship shipNode = (Ship) (ship.instantiate());
        BotProvider provider = (BotProvider) (botProvider.instantiate());

        provider.setGenerator((Generator) getNode("Generator"));

        Vector2 spawn = spawnLocations.get(spawnIndex % spawnLocations.size());

        shipNode.addChild(provider);
        shipNode.setName("Bot" + (1000 + botId));
        shipNode.setProvider(provider);
        shipNode.spawnPosition = new Vector3(spawn.getX(), 0, spawn.getY());

        getNode("Ships").addChild(shipNode);

        botId++;
        spawnIndex++;

        return shipNode;
    }

    /**
     * At the start of a match, creates
     * A collection of both bot and human player ships
     * Sets the camera and playerShip
     * And takes the player from the lobby to the actual match
     */
    @Rpc(
        rpcMode = RpcMode.AUTHORITY,
        sync = Sync.NO_SYNC,
        transferMode = TransferMode.RELIABLE
    )
    @RegisterFunction
    public void startMatch() {
        if (MultiplayerManager.Instance.isServer() && !gameStarted) {
            gameStarted = true;
            ArrayList<PlayerData> players =
                MultiplayerManager.Instance.getSortedPlayerList();

            for (PlayerData player : players) {
                instantiateNewPlayer(player.getPeerId());
            }

            for (int i = 0; i < spawnLocations.size() - players.size(); i++) {
                instantiateNewBot();
            }

            rpc(StringNames.toGodotName("startMatch"));
        }

        gameStarted = true;

        gd.print(MultiplayerManager.Instance.getPeerId() + "starting game");

        ((Button) getNode("Lobby/LobbyMenu/Header/StartGame")).setVisible(true);

        Ship playerShip = (Ship) getNode(
            "Ships/" + getMultiplayer().getUniqueId()
        );

        if (gameStarted && playerShip == null) {
            rpcId(
                1,
                StringNames.toGodotName("instantiateNewPlayer"),
                MultiplayerManager.Instance.getPeerId()
            );
            gd.print(MultiplayerManager.Instance.getPeerId() + "called rpc");
        }

        ((Lobby) getNode("Lobby")).setVisible(false);
    }

    /**
     * When called, returns if match has started
     */
    @RegisterFunction
    public boolean isMatchStarted() {
        return isStarted;
    }

    @Rpc(
        rpcMode = RpcMode.AUTHORITY,
        transferMode = TransferMode.RELIABLE,
        sync = Sync.SYNC
    )
    @RegisterFunction
    public void setupCamera() {
        Ship playerShip = (Ship) getNode(
            "Ships/" + getMultiplayer().getUniqueId()
        );
        gd.print(
            MultiplayerManager.Instance.getPeerId() + "received camera rpc"
        );
        if (playerShip != null) {
            gameCamera.setShip(playerShip);
            gameCamera.setPlayerMode();
            gd.print(
                MultiplayerManager.Instance.getPeerId() + "setting up camera"
            );
        }
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

    @RegisterFunction
    public ArrayList<Vector2> getSpawnPoints() {
        return spawnLocations;
    }
}

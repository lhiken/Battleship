package ui.lobby;

import godot.annotation.Export;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.annotation.RegisterProperty;
import godot.annotation.Rpc;
import godot.annotation.RpcMode;
import godot.api.*;
import godot.core.Callable;
import godot.core.StringNames;
import godot.global.GD;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import main.MatchManager;
import multiplayer.MultiplayerManager;
import multiplayer.PlayerData;
import ui.Hud;

/**
 * The class for Lobby
 * Handles all information regarding the lobby class
 * Includes helper methods to connect players, disconnect players, refresh player screen, and more
 * And whatever in order to make Lobby work properly
 * The lobby right before the match starts
 */
@RegisterClass
public class Lobby extends Control {

    private final GD gd = GD.INSTANCE;
    private PackedScene playerEntryScene;
    private Node playerListNode;

    /**
     * An instance of the matchManager class which gives us information about match manager
     */
    @Export
    @RegisterProperty
    public MatchManager matchManager;

    /**
     * Overrides Godot's built in _ready function
     * Initializes the lobby
     * Acts as a constructor
     * Loads necessary scenes and connects players, sets up labels
     */
    @RegisterFunction
    @Override
    public void _ready() {
        playerEntryScene = gd.load(
            "res://components/ui/lobby/player_entry.tscn"
        );
        playerListNode = getNode("LobbyMenu/PlayerList");

        MultiplayerManager.Instance.playerConnected.connect(
            Callable.create(this, StringNames.toGodotName("onPlayerConnected")),
            0
        );

        MultiplayerManager.Instance.playerDisconnected.connect(
            Callable.create(
                this,
                StringNames.toGodotName("onPlayerDisconnected")
            ),
            0
        );

        RichTextLabel label = (RichTextLabel) getNode(
            "LobbyMenu/Header/BattleshipLabel"
        );
        if (MultiplayerManager.Instance.isServer()) {
            label.setText(MultiplayerManager.Instance.getHostIP());
        } else {
            label.setText("Connected");

            int myId = MultiplayerManager.Instance.getPeerId();
            gd.print("Client (ID: " + myId + ") registering with server");
            rpcId(1, "registerClientWithServer", myId);

            ((Button) getNode("LobbyMenu/Header/Button")).setVisible(false);
        }

        refreshPlayerList();

        ((Label) getParent().getNode("ipDisplay")).setVisible(false);
    }

    /**
     * Function that registers client to the server or lobby
     * Refreshes player list to include the new client
     * @param clientId The id of the client being registered
     */
    @Rpc
    @RegisterFunction
    public void registerClientWithServer(int clientId) {
        if (MultiplayerManager.Instance.isServer()) {
            if (!MultiplayerManager.Instance.hasPlayer(clientId)) {
                MultiplayerManager.Instance.addPlayer(clientId);
            }
            rpc("refreshPlayerList");
        }
    }

    /**
     * Connects a player and adds them to the player list
     * Refreshes player list to include the new client
     * @param peerId The id of the client being registered
     */
    @RegisterFunction
    public void onPlayerConnected(int peerId) {
        if (MultiplayerManager.Instance.isServer()) {
            if (!MultiplayerManager.Instance.hasPlayer(peerId)) {
                MultiplayerManager.Instance.addPlayer(peerId);
            }

            rpc("refreshPlayerList");
        }

        refreshPlayerList();
    }

    /**
     * Function that acts when a player disconnects
     * Refreshes player list to remove them
     * @param peerId the peer id that disconnected
     */
    @RegisterFunction
    public void onPlayerDisconnected(int peerId) {
        refreshPlayerList();

        if (MultiplayerManager.Instance.isServer()) {
            rpc("refreshPlayerList");
        }
    }

    /**
     * Function that starts the match
     * Prepares to start the match
     */
    @Rpc(rpcMode = RpcMode.AUTHORITY)
    @RegisterFunction
    public void startMatch() {

        gd.print("start match");
        matchManager.startMatch();

        CanvasLayer layer = (CanvasLayer) getParent().getNode("Hud");
        layer.setVisible(true);

        if (MultiplayerManager.Instance.isServer()) {
            ((Label) getParent().getNode("ipDisplay")).setText(
                    MultiplayerManager.Instance.getHostIP()
                );
            ((Label) getParent().getNode("ipDisplay")).setVisible(true);
        }
    }

    @RegisterFunction
    @Override
    public void _input(InputEvent event) {
        if (event.isActionPressed("hide_lobby")) {
            if (isVisible()) {
                setVisible(false);
            } else {
                setVisible(true);
            }
        }
    }

    /**
     * Refreshes the player list
     * Finds all instances of players in the arraylist and then adds them to the player entry scene
     */
    @Rpc
    @RegisterFunction
    public void refreshPlayerList() {
        ArrayList<PlayerData> allPlayers =
            MultiplayerManager.Instance.getSortedPlayerList();

        for (int i = playerListNode.getChildCount() - 1; i >= 0; i--) {
            Node child = playerListNode.getChild(i);
            child.queueFree();
        }

        int i = 0;
        for (PlayerData playerData : allPlayers) {
            PlayerEntry newPlayer =
                (PlayerEntry) playerEntryScene.instantiate();
            newPlayer.setPeerId(playerData.getPeerId());
            newPlayer.setPoints(playerData.getPoints());
            newPlayer.setIndex(i);
            playerListNode.addChild(newPlayer);
            i++;
        }
    }
}

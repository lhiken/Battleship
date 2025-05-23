package ui.lobby;

import godot.annotation.Export;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.annotation.RegisterProperty;
import godot.annotation.Rpc;
import godot.api.Button;
import godot.api.Control;
import godot.api.Node;
import godot.api.PackedScene;
import godot.api.RichTextLabel;
import godot.core.Callable;
import godot.core.StringNames;
import godot.global.GD;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import main.MatchManager;
import multiplayer.MultiplayerManager;
import multiplayer.PlayerData;

@RegisterClass
public class Lobby extends Control {

    private final GD gd = GD.INSTANCE;
    private PackedScene playerEntryScene;
    private Node playerListNode;

    @Export
    @RegisterProperty
    public MatchManager matchManager;

    /** _ready
     * initializes the lobby
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
            String addr = "Hosting...";
            try {
                addr = Inet4Address.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                gd.print(e.toString());
            }
            label.setText(addr);
        } else {
            label.setText("Connected");

            int myId = MultiplayerManager.Instance.getPeerId();
            gd.print("Client (ID: " + myId + ") registering with server");
            rpcId(1, "registerClientWithServer", myId);

            ((Button) getNode("LobbyMenu/Header/Button")).setVisible(false);
        }

        refreshPlayerList();
    }

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

    @RegisterFunction
    public void onPlayerDisconnected(int peerId) {
        refreshPlayerList();

        if (MultiplayerManager.Instance.isServer()) {
            rpc("refreshPlayerList");
        }
    }

    @RegisterFunction
    public void startMatch() {
        gd.print("start match");
        matchManager.startMatch();
    }

    @Rpc
    @RegisterFunction
    public void refreshPlayerList() {
        gd.print("Refreshing player list");

        ArrayList<PlayerData> allPlayers =
            MultiplayerManager.Instance.getSortedPlayerList();
        gd.print(
            "Current player count in MultiplayerManager: " + allPlayers.size()
        );
        for (PlayerData p : allPlayers) {
            gd.print(
                "  Player ID: " + p.getPeerId() + " Points: " + p.getPoints()
            );
        }

        for (int i = playerListNode.getChildCount() - 1; i >= 0; i--) {
            Node child = playerListNode.getChild(i);
            child.queueFree();
        }

        int i = 0;
        for (PlayerData playerData : allPlayers) {
            gd.print("Adding player entry for ID: " + playerData.getPeerId());
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

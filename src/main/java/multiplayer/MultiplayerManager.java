package multiplayer;

import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.ENetMultiplayerPeer;
import godot.api.MultiplayerAPI;
import godot.api.Node;
import godot.api.Node3D;
import godot.api.PackedScene;
import godot.core.Callable;
import godot.core.StringNames;
import godot.global.GD;
import ui.StartMenu;

@RegisterClass
public class MultiplayerManager extends Node {

    public StartMenu menu; // theres prolly something wrong with this...

    private static final GD gd = GD.INSTANCE;

    private final int serverPort = 8080;

    @RegisterFunction
    @Override
    public void _ready() {
        menu = (StartMenu) getNode("StartMenu");
        gd.print(menu);
    }

    @RegisterFunction
    public void initiateHost() {
        gd.print("initializing host");

        // create server
        ENetMultiplayerPeer peer = new ENetMultiplayerPeer();
        peer.createServer(serverPort);

        MultiplayerAPI multiplayer = getMultiplayer();
        multiplayer.setMultiplayerPeer(peer);
        multiplayer
            .getPeerConnected()
            .connect(
                Callable.create(this, StringNames.toGodotName("addPlayer")),
                0
            );

        multiplayer
            .getPeerDisconnected()
            .connect(
                Callable.create(this, StringNames.toGodotName("deletePlayer")),
                0
            );

        menu.onMultiplayerConnect();

        initializeMultiplayerMatch();
    }

    @RegisterFunction
    public void addPlayer(int id) {
        gd.print("added player " + id);
    }

    @RegisterFunction
    public void removePlayer(int id) {
        gd.print("removed player " + id);
    }

    @RegisterFunction
    public void initiateClient(String serverIP) {
        gd.print("initializing client");

        MultiplayerAPI multiplayer = getMultiplayer();
        // ts does not work
        // multiplayer
        //     .getConnectionFailed()
        //     .connect(
        //         Callable.create(
        //             this,
        //             StringNames.toGodotName("clientConnectionFailed")
        //         ),
        //         0
        //     );
        // multiplayer
        //     .getConnectedToServer()
        //     .connect(
        //         Callable.create(
        //             this,
        //             StringNames.toGodotName("clientConnectionSuccess")
        //         ),
        //         0
        //     );

        ENetMultiplayerPeer peer = new ENetMultiplayerPeer();
        peer.createClient(serverIP, serverPort);

        multiplayer.setMultiplayerPeer(peer);

        menu.onMultiplayerConnect();

        initializeMultiplayerMatch();
    }

    private void initializeMultiplayerMatch() {
        PackedScene match = gd.load("res://components/main/match.tscn");
        getParent().getNode("Props").queueFree();
        getParent().getNode("RenderTarget").queueFree();
        Node m = match.instantiate();
        getParent().addChild(m);
    }
    // // exists because signals suck
    // @RegisterFunction
    // public void clientConnectionFailed() {
    //     gd.print("what the sigma");
    //     menu.onMultiplayerConnect();
    // }

    // // exists because signals suck
    // @RegisterFunction
    // public void clientconnectionSuccess() {
    //     gd.print("what the sigma");
    //     menu.onMultiplayerConnect();
    // }
}

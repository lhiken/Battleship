package multiplayer;

import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.annotation.RegisterSignal;
import godot.annotation.Rpc;
import godot.annotation.RpcMode;
import godot.annotation.Sync;
import godot.annotation.TransferMode;
import godot.api.ENetMultiplayerPeer;
import godot.api.MultiplayerAPI;
import godot.api.Node;
import godot.core.Callable;
import godot.core.Signal1;
import godot.core.StringNames;
import godot.global.GD;
import java.util.ArrayList;
import java.util.HashMap;

@RegisterClass
public class MultiplayerManager extends Node {

    public static MultiplayerManager Instance;
    private static final GD gd = GD.INSTANCE;
    private final int serverPort = 7440;
    private ENetMultiplayerPeer peer;
    private MultiplayerAPI multiplayer;

    private HashMap<Integer, PlayerData> playerData;

    @RegisterSignal
    public Signal1<Boolean> multiplayerConnected = Signal1.create(
        this,
        "multiplayerConnected"
    );

    @RegisterSignal
    public Signal1<Integer> playerConnected = Signal1.create(
        this,
        "playerConnected"
    );

    @RegisterSignal
    public Signal1<Integer> playerDisconnected = Signal1.create(
        this,
        "playerDisconnected"
    );

    @RegisterFunction
    @Override
    public void _ready() {
        Instance = this;

        playerData = new HashMap<>();

        multiplayer = getMultiplayer();
        multiplayer.getMultiplayerPeer().close();
        multiplayer
            .getPeerConnected()
            .connect(
                Callable.create(this, StringNames.toGodotName("addPlayer")),
                0
            );

        multiplayer
            .getPeerDisconnected()
            .connect(
                Callable.create(this, StringNames.toGodotName("removePlayer")),
                0
            );

        multiplayer
            .getConnectionFailed()
            .connect(
                Callable.create(
                    this,
                    StringNames.toGodotName("clientConnectionFailed")
                ),
                0
            );

        multiplayer
            .getConnectedToServer()
            .connect(
                Callable.create(
                    this,
                    StringNames.toGodotName("clientConnectionSuccess")
                ),
                0
            );
    }

    @RegisterFunction
    public void initiateHost() {
        gd.print("initializing host");
        // create server
        peer = new ENetMultiplayerPeer();
        peer.createServer(serverPort);
        multiplayer.setMultiplayerPeer(peer);

        addPlayer(1);

        multiplayerConnected.emit(true);
    }

    @Rpc(
        rpcMode = RpcMode.AUTHORITY,
        sync = Sync.NO_SYNC,
        transferMode = TransferMode.RELIABLE
    )
    @RegisterFunction
    public void addPlayer(int id) {
        gd.print("added player " + id);
        playerData.put(id, new PlayerData(id));
        playerConnected.emit(id);
        if (isServer()) rpc(StringNames.toGodotName("addPlayer"), id);
    }

    @Rpc(
        rpcMode = RpcMode.AUTHORITY,
        sync = Sync.NO_SYNC,
        transferMode = TransferMode.RELIABLE
    )
    @RegisterFunction
    public void removePlayer(int id) {
        gd.print("removed player " + id);
        playerData.remove(id);
        playerDisconnected.emit(id);
        if (isServer()) rpc(StringNames.toGodotName("removePlayer"), id);
    }

    @RegisterFunction
    public void initiateClient(String serverIP) {
        gd.print("initializing client to " + serverIP + ":" + serverPort);

        peer = new ENetMultiplayerPeer();
        peer.createClient(serverIP, serverPort);
        multiplayer.setMultiplayerPeer(peer);

        gd.print("Client connection attempt initiated");
    }

    @RegisterFunction
    public void clientConnectionFailed() {
        gd.print("Connection to server failed");
        multiplayerConnected.emit(false);
    }

    @RegisterFunction
    public void clientConnectionSuccess() {
        gd.print("Successfully connected to server");
        multiplayerConnected.emit(true);
    }

    @RegisterFunction
    public PlayerData getPlayerData(int peerId) {
        return playerData.get(peerId);
    }

    @RegisterFunction
    public ArrayList<PlayerData> getSortedPlayerList() {
        ArrayList<PlayerData> list = new ArrayList<>();
        for (PlayerData data : playerData.values()) {
            list.add(data);
        }

        list.sort((a, b) -> Integer.compare(b.getPoints(), a.getPoints()));

        return list;
    }

    @Rpc(
        rpcMode = RpcMode.ANY,
        sync = Sync.NO_SYNC,
        transferMode = TransferMode.RELIABLE
    )
    @RegisterFunction
    public String getHostIP() {
        if (peer != null && peer.getPeer(1) != null) {
            return peer.getPeer(1).getRemoteAddress();
        }
        return "Unknown";
    }

    @RegisterFunction
    public int getPeerId() {
        return getMultiplayer().getUniqueId();
    }

    @RegisterFunction
    public boolean isServer() {
        return getMultiplayer().isServer();
    }

    @RegisterFunction
    public void cleanup() {
        if (peer != null) {
            peer.close();
            peer = null;
        }
    }

    @RegisterFunction
    public boolean hasPlayer(int peerId) {
        return playerData.containsKey(peerId);
    }
}

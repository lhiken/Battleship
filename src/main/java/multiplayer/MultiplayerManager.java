package multiplayer;

import entity.Ship;
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
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;

/**
 * Handles multiplayer for the entire game
 * Contains godot signals that tell other classes
 * when a player connects/disconnects
 */
@RegisterClass
public class MultiplayerManager extends Node {

    /**
     * A MultiplayerManager Instance for nodes in
     * other classes to get a reference to in the
     * singleton pattern
     */
    public static MultiplayerManager Instance;

    /**
     * A referencce to godot's global functions class
     */
    private static final GD gd = GD.INSTANCE;

    /**
     * The port the multiplayer server will communicate on
     */
    private final int serverPort = 7440;

    /**
     * A Godot multiplayer peer object for the local client
     */
    private ENetMultiplayerPeer peer;

    /**
     * A Godot multiplayer API object for the local client
     */
    private MultiplayerAPI multiplayer;

    /**
     * A map that associates each player's peerId with their
     * player data, maintained by the host
     */
    private HashMap<Integer, PlayerData> playerData;

    /**
     * A Godot signal for multiplayer connections
     * Other classes can hook on to this signal and call functions when
     * this signal is emitted
     */
    @RegisterSignal
    public Signal1<Boolean> multiplayerConnected = Signal1.create(
        this,
        "multiplayerConnected"
    );

    /**
     * A Godot signal for the server when players connect
     * Other classes can hook on to this signal and call functions when
     * this signal is emitted
     */
    @RegisterSignal
    public Signal1<Integer> playerConnected = Signal1.create(
        this,
        "playerConnected"
    );

    /**
     * A Godot signal for the server when players disconnect
     * Other classes can hook on to this signal and call functions when
     * this signal is emitted
     */
    @RegisterSignal
    public Signal1<Integer> playerDisconnected = Signal1.create(
        this,
        "playerDisconnected"
    );

    /**
     * Godot function that runs when the node is loaded into the game
     * Initializes fields and connects signals to associated functions
     */
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

    /**
     * Function for handling initializing the game in host mode
     * Creates a new multiplayer peer as a server (host)
     */
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

    /**
     * Adds a new player to the game by placing a new set of
     * playerData into the server's playerData map
     *
     * @param id the peerId of the new player
     */
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

    /**
     * Removes a player from the game by removing their set of
     * playerData from the server's playerData map
     *
     * @param id the peer id of the removed player
     */
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

    /**
     * Function for handling initializing the game in client mode
     * Creates a new multiplayer peer as a client
     *
     * @param serverIP the ip address of the server
     */
    @RegisterFunction
    public void initiateClient(String serverIP) {
        gd.print("initializing client to " + serverIP + ":" + serverPort);

        peer = new ENetMultiplayerPeer();
        peer.createClient(serverIP, serverPort);
        multiplayer.setMultiplayerPeer(peer);

        gd.print("Client connection attempt initiated");
    }

    /**
     * Function connected to the client connection failed signal,
     * and emits a multiplayerConnected signal with the value false
     */
    @RegisterFunction
    public void clientConnectionFailed() {
        gd.print("Connection to server failed");
        multiplayerConnected.emit(false);
    }

    /**
     * Function connected to the client connection success signal,
     * and emits a multiplayerConnected signal with the value true
     */
    @RegisterFunction
    public void clientConnectionSuccess() {
        gd.print("Successfully connected to server");
        multiplayerConnected.emit(true);
    }

    /**
     * Getter for player data from the playerData map
     *
     * @param peerId the peerId of the player we are getting data from
     */
    @RegisterFunction
    public PlayerData getPlayerData(int peerId) {
        return playerData.get(peerId);
    }

    /**
     * Gets the playerData from the playerData map as an arraylist
     *
     * @return arraylist with playerData of all players
     */
    @RegisterFunction
    public ArrayList<PlayerData> getSortedPlayerList() {
        ArrayList<PlayerData> list = new ArrayList<>();
        for (PlayerData data : playerData.values()) {
            list.add(data);
        }

        list.sort((a, b) -> Integer.compare(b.getPoints(), a.getPoints()));

        return list;
    }

    /**
     * gets the host ip
     * @return the host's ip address
     */
    @Rpc(
        rpcMode = RpcMode.ANY,
        sync = Sync.NO_SYNC,
        transferMode = TransferMode.RELIABLE
    )
    @RegisterFunction
    public String getHostIP() {
        if (isServer()) {
            try {
                Enumeration<NetworkInterface> interfaces =
                    NetworkInterface.getNetworkInterfaces();

                while (interfaces.hasMoreElements()) {
                    NetworkInterface iface = interfaces.nextElement();
                    if (
                        iface.isLoopback() || !iface.isUp() || iface.isVirtual()
                    ) continue;

                    Enumeration<InetAddress> addresses =
                        iface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        if (addr instanceof Inet4Address) {
                            return addr.getHostAddress();
                        }
                    }
                }
                System.out.println("No LAN IP found");
            } catch (SocketException e) {
                return "Unknown";
            }
        }
        return "Connected";
    }

    /**
     * getter for peer id of client/server
     * @return peer id
     */
    @RegisterFunction
    public int getPeerId() {
        return getMultiplayer().getUniqueId();
    }

    /**
     * checks if the client is a server
     * @return is server true/false
     */
    @RegisterFunction
    public boolean isServer() {
        return getMultiplayer().isServer();
    }

    /**
     * cleans up when peer is closed
     */
    @RegisterFunction
    public void cleanup() {
        if (peer != null) {
            peer.close();
            peer = null;
        }
    }

    /**
     * checks if a player is registered in playerData
     * @param peerId the peerId being checked
     * @return peerId's registration status
     */
    @RegisterFunction
    public boolean hasPlayer(int peerId) {
        return playerData.containsKey(peerId);
    }

    /**
     * handles bullet damage as the host
     * @param ownerId the ship who shot the bullet
     * @param targetShip the ship who received the bullet
     * @param damage the damage being applied
     */
    @RegisterFunction
    public void invokeBulletDamage(
        int ownerId,
        Ship targetShip,
        double damage
    ) {
        if (multiplayer.isServer()) {
            targetShip.setHealth(targetShip.getHealth() - damage);
            PlayerData data = playerData.get(ownerId);
            if (data != null) {
                data.setPoints(data.getPoints() + 50);
            }
        }
    }
}

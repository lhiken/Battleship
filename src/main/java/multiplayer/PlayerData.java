package multiplayer;

/**
 * contains player associated data
 */
public class PlayerData {

    /**
     * username of player
     */
    private String username;

    /**
     * multiplayer peerId
     */
    private int peerId;

    /**
     * player's points
     */
    private int points;

    /**
     * initializes player data fields
     * @param peerId peerId of new player
     */
    public PlayerData(int peerId) {
        this.peerId = peerId;
        String idStr = String.valueOf(peerId);
        String shortId = idStr.length() >= 3 ? idStr.substring(0, 3) : idStr;
        this.username = "Player" + shortId;
        this.points = 0;
    }

    /**
     * gets the username
     * @return username string
     */
    public String getUsername() {
        return username;
    }

    /**
     * gets the multiplayer peer id
     * @return peer id
     */
    public int getPeerId() {
        return peerId;
    }

    /**
     * gets the points
     * @return the current number of points
     */
    public int getPoints() {
        return points;
    }

    /**
     * sets the points
     * @param points new amount of points
     */
    public void setPoints(int points) {
        this.points = points;
    }

    /**
     * adds some points
     * @param points points being added
     */
    public void addPoints(int points) {
        this.points += points;
    }

    /**
     * string representation of playerData
     * @return string in the format of Player {"username", "peerId", "points"}
     */
    @Override
    public String toString() {
        return "Player { " + username + ", " + peerId + ", " + points + "}";
    }
}

package multiplayer;

public class PlayerData {

    private String username;
    private int peerId;
    private int points;

    public PlayerData(int peerId) {
        this.peerId = peerId;
        String idStr = String.valueOf(peerId);
        String shortId = idStr.length() >= 3 ? idStr.substring(0, 3) : idStr;
        this.username = "Player" + shortId;
        this.points = 0;
    }

    public String getUsername() {
        return username;
    }

    public int getPeerId() {
        return peerId;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public void addPoints(int points) {
        this.points += points;
    }

    public void subtractPoints(int points) {
        this.points -= points;
    }

    @Override
    public String toString() {
        return "Player { " + username + ", " + peerId + ", " + points + "}";
    }
}

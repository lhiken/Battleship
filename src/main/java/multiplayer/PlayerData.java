package multiplayer;

public class PlayerData {

    private String username;
    private int peerId;
    private int points;
    private double health;

    public PlayerData(int peerId) {
        this.peerId = peerId;
        String idStr = String.valueOf(peerId);
        String shortId = idStr.length() >= 3 ? idStr.substring(0, 3) : idStr;
        this.username = "Player" + shortId;
        this.points = 0;
        this.health = 1;
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

    public double getHealth() {
        return health;
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

    public void setHealth(double health) {
        this.health = health;
    }

    public void addHealth(double health) {
        this.health += health;
    }

    public void subtractHealth(double health) {
        this.health -= health;
    }

    @Override
    public String toString() {
        return "Player { " + username + ", " + peerId + ", " + points + "}";
    }
}

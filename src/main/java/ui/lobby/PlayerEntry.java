package ui.lobby;

import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.Panel;
import godot.api.RichTextLabel;
import godot.global.GD;
import multiplayer.MultiplayerManager;

/** PlayerEntry
 * Represents a player in the lobby menu
 */
@RegisterClass
public class PlayerEntry extends Panel {

    private GD gd = GD.INSTANCE;

    private int peerId;
    private int index;
    private int points;
    private String label;

    private boolean isPlayer = false;

    /**
     * Calls update when it spawns in
     */
    @RegisterFunction
    @Override
    public void _enterTree() {
        update();
    }

    /**
     * Sets peerId to input parameter, and calls update
     * @param peerId is the parameter to set current peerId to
     */
    @RegisterFunction
    public void setPeerId(int peerId) {
        this.peerId = peerId;
        update();
    }

    /**
     * Sets index to input parameter, and calls update
     * @param index is the parameter to set current index to
     */
    @RegisterFunction
    public void setIndex(int index) {
        this.index = index;
        update();
    }

    /**
     * Sets points to input parameter, and calls update
     * @param points is the parameter to set current points to
     */
    @RegisterFunction
    public void setPoints(int points) {
        this.points = points;
        update();
    }

    /**
     * Labels Id depending on if player is host or others
     */
    @RegisterFunction
    public void update() {
        int realId = MultiplayerManager.Instance.getPeerId();
        if (realId == peerId) {
            label = "You";
            if (MultiplayerManager.Instance.isServer()) {
                label += " (Host)";
            }

            isPlayer = true;
        } else {
            String idStr = String.valueOf(peerId);
            String shortId = idStr.length() >= 3
                ? idStr.substring(0, 3)
                : idStr;
            label = "Player" + shortId;
            if (peerId == 1) {
                label = "Host";
            }
        }
        points = MultiplayerManager.Instance.getPlayerData(peerId).getPoints();
        gd.print("id: " + peerId + " points: " + points);

        RichTextLabel playerLabel = (RichTextLabel) getNode("PlayerLabel");
        RichTextLabel pointsLabel = (RichTextLabel) getNode("PointsLabel");

        playerLabel.setText("#" + (index + 1) + " | " + label);
        pointsLabel.setText(points + " Pts");
    }
}

package ui.lobby;

import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.Panel;
import godot.api.RichTextLabel;
import multiplayer.MultiplayerManager;

/** PlayerEntry
 * Represents a player in the lobby menu
 */
@RegisterClass
public class PlayerEntry extends Panel {

    private int peerId;
    private int index;
    private int points;
    private String label;

    private boolean isPlayer = false;

    @RegisterFunction
    @Override
    public void _enterTree() {
        update();
    }

    @RegisterFunction
    public void setPeerId(int peerId) {
        this.peerId = peerId;
        update();
    }

    @RegisterFunction
    public void setIndex(int index) {
        this.index = index;
        update();
    }

    @RegisterFunction
    public void setPoints(int points) {
        this.points = points;
        update();
    }

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
        points = 0;

        RichTextLabel playerLabel = (RichTextLabel) getNode("PlayerLabel");
        RichTextLabel pointsLabel = (RichTextLabel) getNode("PointsLabel");

        playerLabel.setText("#" + (index + 1) + " | " + label);
        pointsLabel.setText(points + " Pts");
    }
}

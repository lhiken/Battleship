package map;

import godot.annotation.Export;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.annotation.RegisterProperty;
import godot.api.Node;
import godot.api.Node3D;
import godot.core.Vector3;

/** Ocean
 *  this class generates ocean tiles of 128x128 upon
 *  _ready so they can be frustum culled for optimization
 *  purposes
 */
@RegisterClass
public class Ocean extends Node3D {
    @Export
    @RegisterProperty
    public int tileX = 4;
    @Export
    @RegisterProperty
    public int tileY = 4;
    @Export
    @RegisterProperty
    public double tileSize = 128;

    @RegisterFunction
    @Override
    public void _ready() {
        Node3D editorTile = (Node3D) getNode("EditorPlane");
        assert editorTile != null;
        editorTile.setVisible(false);

        Node oceanTile = getNode("OceanPlane");
        Node3D tile = (Node3D) oceanTile;
        assert tile != null;

        Vector3 translation = new Vector3((tileX - 1) * tileSize / -2.0, 0, (tileY - 1) * tileSize / -2.0);
        tile.translate(translation);
        tile.setVisible(false);

        for (int r = 0; r < tileX; r++) {
            for (int c = 0; c < tileY; c++) {
                Node3D newTile = (Node3D) oceanTile.duplicate();
                assert newTile != null;

                addChild(newTile);
                newTile.translate(
                    new Vector3(r * tileSize, 0, c * tileSize)
                );
                newTile.setVisible(true);
            }
        }
    }

}
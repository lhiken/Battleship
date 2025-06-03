package map;

import godot.annotation.Export;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.annotation.RegisterProperty;
import godot.api.MeshInstance3D;
import godot.api.Node3D;
import godot.api.ShaderMaterial;
import godot.api.SubViewport;
import godot.api.Texture2D;

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
        MeshInstance3D water = (MeshInstance3D) getNode("OceanPlane");
        Texture2D sim_tex = ((SubViewport) getNode("Simulation")).getTexture();

        ShaderMaterial mat = (ShaderMaterial) water
            .getMesh()
            .surfaceGetMaterial(0);
        mat.setShaderParameter("simulation", sim_tex);

        Node3D editorPlane = (Node3D) getNode("EditorPlane");
        editorPlane.setVisible(false);

        Node3D oceanPlane = (Node3D) getNode("OceanPlane");
        oceanPlane.setVisible(true);
    }
}

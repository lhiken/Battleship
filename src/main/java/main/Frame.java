package main;

import godot.annotation.Export;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.annotation.RegisterProperty;
import godot.api.Camera3D;
import godot.api.SubViewport;
import godot.api.TextureRect;
import godot.core.Vector2;
import godot.core.Vector2i;
import godot.global.GD;

/** Frame
 * the class for the textureRect that renders the main scene
 */
@RegisterClass
public class Frame extends TextureRect {
    // include this line everywhere bc godot's methods use it
    private static final GD gd = GD.INSTANCE;
    private SubViewport viewport;
    private Vector2 windowSize;
    private double aspectRatio;

    @Export
    @RegisterProperty
    public Camera3D camera;
    @Export
    @RegisterProperty
    public int targetHeight;

    @RegisterFunction
    @Override
    public void _ready() {
        viewport = (SubViewport) getNodeOrNull("Viewport");

        updateViewport();

        setTexture(viewport.getTexture());

        camera.getParent().removeChild(camera);
        viewport.addChild(camera);
        camera.setCurrent(true);
    }

    @RegisterFunction
    @Override
    public void _process(double delta) {
        updateViewport();
    }

    private void updateViewport() {
        windowSize = getViewport().getVisibleRect().getSize();
        aspectRatio = windowSize.getX() / windowSize.getY();
        int targetWidth = Math.round((int) (targetHeight * aspectRatio));
        viewport.setSize(new Vector2i(targetWidth, targetHeight));
    }
}
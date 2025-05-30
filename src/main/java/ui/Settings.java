package ui;

import godot.annotation.Export;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.annotation.RegisterProperty;
import godot.api.Control;
import godot.api.InputEvent;
import godot.api.MultiplayerAPI;
import godot.global.GD;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import multiplayer.MultiplayerManager;

/**
 * The settings menu that is opened when pressed on start screen
 */
@RegisterClass
public class Settings extends Control {

	private GD gd = GD.INSTANCE;
	private Control Options;

	/**
	 * Manages the multiplayer
	 */
	@RegisterProperty
	@Export
	public MultiplayerManager multiplayerManager;

	/**
	 * When game starts, settings is not visible
	 */
	@RegisterFunction
	@Override
	public void _ready() {
		Options = (Control) getNode("Options");
		Options.setVisible(false);
	}

	/**
	 * When settings button is clicked, the menu becomes visible
	 */
	@RegisterFunction
	public void _onSettingsClicked() {
		Options.setVisible(true);
		gd.print("yay clicked");
	}

	/**
	 * When the return button on the settings
	 * menu is clicked, make settings invisible
	 */
	@RegisterFunction
	public void _onReturnClicked() {
		if (Options.isVisible()
		) Options.setVisible(false);
	}
}

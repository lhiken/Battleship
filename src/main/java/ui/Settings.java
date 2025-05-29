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

@RegisterClass
public class Settings extends Control {

	private GD gd = GD.INSTANCE;
	private Control Options;
b
	@RegisterProperty
	@Export
	public MultiplayerManager multiplayerManager;

	@RegisterFunction
	@Override
	public void _ready() {
		Options = (Control) getNode("Options");
		Options.setVisible(false);
	}

	@RegisterFunction
	public void _onSettingsClicked() {
		Options.setVisible(true);
		gd.print("yay clicked");
	}
	
	@RegisterFunction
	public void _onReturnClicked() {
		if (Options.isVisible()
		) Options.setVisible(false);
	}
}

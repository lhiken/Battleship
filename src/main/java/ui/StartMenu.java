package ui;

import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.Control;
import godot.api.InputEvent;
import godot.api.MultiplayerAPI;
import godot.core.Callable;
import godot.core.StringNames;
import godot.global.GD;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import multiplayer.MultiplayerManager;

@RegisterClass
public class StartMenu extends Control {

	private GD gd = GD.INSTANCE;
	private Control ipInput;
	private Control Options;

	@RegisterFunction
	@Override
	public void _ready() {
		ipInput = (Control) getNode("IPInput");
		ipInput.setVisible(false);
		MultiplayerManager.Instance.multiplayerConnected.connect(
			Callable.create(
				this,
				StringNames.toGodotName("onMultiplayerConnect")
			),
			1
		);
	}

	@RegisterFunction
	public void _onHostClicked() {
		MultiplayerManager.Instance.initiateHost();
	}

	@RegisterFunction
	public void _onJoinClicked() {
		ipInput.setVisible(true);
	}

	@RegisterFunction
	public void _onIpSubmit(String text) {
		Pattern pat = Pattern.compile(
			"^(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
		);
		Matcher mat = pat.matcher(text);

		if (!mat.matches()) {
			gd.print("bad ip");
			return;
		}

		MultiplayerManager.Instance.initiateClient(text);
	}

	@RegisterFunction
	@Override
	public void _input(InputEvent event) {
		if (
			event.isActionPressed("ui_cancel") && ipInput.isVisible()
		) ipInput.setVisible(false);
	}

	@RegisterFunction
	public void onMultiplayerConnect(Boolean success) {
		MultiplayerAPI multiplayer = getMultiplayer();
		gd.print(
			"connected: " +
			success +
			" with peer id " +
			multiplayer.getUniqueId() +
			" as server: " +
			multiplayer.isServer()
		);
	}
	
}

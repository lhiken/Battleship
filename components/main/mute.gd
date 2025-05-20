extends CheckBox

@export
var bus_name: String

var bus_index:int

func _on_mute_toggled(toggled_on: bool) -> void:
	bus_index = AudioServer.get_bus_index(bus_name)
	AudioServer.set_bus_mute(bus_index, toggled_on)

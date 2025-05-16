extends VBoxContainer

var entry = preload("res://components/ui/lobby/player_entry.tscn")

var len;

func _ready() -> void:
	len = multiplayer.get_peers().size()
	print("loaded list ", len)
	update_entries()

func _process(delta: float) -> void:
	if multiplayer.get_peers().size() != len:
		update_entries()

func update_entries():
	for child in get_children():
		remove_child(child)
		child.queue_free()

	var peers = multiplayer.get_peers()
	for peer_id in peers:
		print(peer_id)
		var new_entry = entry.instantiate()
		new_entry.peer_id = peer_id
		add_child(new_entry)

extends RichTextLabel

# Called when the node enters the scene tree for the first time.
func _ready() -> void:
	var ip_address :String

	if OS.has_feature("windows"):
		if OS.has_environment("COMPUTERNAME"):
			ip_address =  IP.resolve_hostname(str(OS.get_environment("COMPUTERNAME")),1)
	else:
		if OS.has_environment("HOSTNAME"):
			ip_address =  IP.resolve_hostname(str(OS.get_environment("HOSTNAME")),1)
		
	text = "" if !multiplayer.is_server() else ip_address

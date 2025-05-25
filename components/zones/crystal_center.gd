extends MeshInstance3D

var elapsed = 0

# Called every frame. 'delta' is the elapsed time since the previous frame.
func _process(delta: float) -> void:
	elapsed += delta
	rotate(Vector3.UP, delta)
	position = Vector3(0, sin(elapsed) * 0.25 + 2.5, 0)

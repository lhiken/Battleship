shader_type spatial;
render_mode cull_disabled;

uniform vec3 emission_color : source_color;
uniform vec3 zone_color : source_color;

void fragment() {
	float height = (INV_VIEW_MATRIX * vec4(VERTEX, 1.0)).y;

	if (height > 1.0 || height < 0.0) discard;

	ALBEDO = zone_color;
	EMISSION = vec3(1.0 - height) * emission_color;
	ALPHA = 1.0 - height;
}

//void light() {
//	// Called for every pixel for every light affecting the material.
//	// Uncomment to replace the default light processing function with this one.
//}

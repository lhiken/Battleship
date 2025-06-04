shader_type spatial;

uniform vec3 emission_color : source_color;
uniform vec3 zone_color : source_color;

void vertex() {
	// Called for every vertex the material is visible on.
}

void fragment() {

	EMISSION = vec3(1.0) * emission_color;
	ALBEDO = zone_color;

	// Called for every pixel the material is visible on.
}

//void light() {
//	// Called for every pixel for every light affecting the material.
//	// Uncomment to replace the default light processing function with this one.
//}

shader_type spatial;
render_mode depth_prepass_alpha; //ngl i dont know why i need this

// standard edge detection "inspired by" (stolen from) david holland

// basically we are reading the position and direction (normal)
// of each pixel near the current one and seeing if theres any difference
// between them, and if there is, then we are marking it as an edge
// this is surprisingly uncomplicated!!

// after the edge detection we apply some textures and calculations and
// other random stuff to make things colored differently

uniform sampler2D depth_texture : hint_depth_texture;
uniform sampler2D normal_texture : hint_normal_roughness_texture;
uniform sampler3D color_noise;

uniform vec3 outline_color : source_color;
uniform vec3 edge_color : source_color;
uniform vec3 base_color : source_color;
uniform vec3 terrain_color : source_color;

uniform float terrain_threshold : hint_range(0.0, 1.0) = 0.25;

vec3 _get_point_position(vec2 screen_uv, float depth, mat4 inv_projection_matrix) {
	vec4 point = inv_projection_matrix * vec4(screen_uv * 2.0 - 1.0, depth, 1.0);
	return point.xyz / point.w;
}
#define get_point_position(uv, depth_texture) _get_point_position(uv, texture(depth_texture, uv).r, INV_PROJECTION_MATRIX);

float remap(float value, float low1, float high1, float low2, float high2) {
    return low2 + (value - low1) * (high2 - low2) / (high1 - low1);
}

void fragment() {
	vec2 texel_size = 1.0 / VIEWPORT_SIZE;
	// takes the normals and positions from neighboring pixels
	vec2 offset[5] = {
					 vec2(-1, 0),
		vec2(0, -1), vec2( 0, 0), vec2(0, 1),
					 vec2( 1, 0)
	};
	vec3 normal[5];
	vec3 position[5];

	vec3 world_normal = (INV_VIEW_MATRIX * vec4(NORMAL, 0.0)).xyz;
	float upwardness = 1.0 - step(dot(world_normal, vec3(0, 1, 0)), terrain_threshold);
	vec3 base_albedo = mix(base_color, terrain_color, upwardness);

	ALBEDO = base_albedo;

	for (int i = 0; i < 5; i++) {
		vec2 uv = SCREEN_UV + offset[i] * texel_size;
		position[i] = get_point_position(uv, depth_texture);
		normal[i] = texture(normal_texture, uv).xyz * 2.0 - 1.0;
	}

	// gets the threshold and stuff from the current pixel, probably not even needed
	// this is straight up from david hollands code btw but it works and its the best way
	// of doing it so why should i settle for anything lesser
	float depth_normal_threshold = 0.5;
	float depth_normal_bias = remap(
		max(0.25, 1.0 - normal[2].z),
		depth_normal_threshold, 1.0,
		1.0, 2.0
	);
	float dt = depth_normal_threshold * depth_normal_bias;

	float convex_normal_threshold = 0.5;
	float convex_normal_bias = remap(
		min(normal[2].z, convex_normal_threshold),
		0.0, convex_normal_threshold,
		2.0, 0.25
	);
	float ct = convex_normal_threshold * convex_normal_bias;

	bool edge = false;
	for (int i = 0; i < 5; i++) {
		if (i == 2) continue;
		vec3 cp = cross(normal[2], normal[i]);
		float convexity = dot(cp, vec3(offset[i].yx, 0));
		float norm_bias = i < 2 ? 1e-5 : 0.0;
		edge =
			abs(position[i].z - position[2].z) < dt
			&& normal[i].z + 1.5 > normal[2].z
			&& convexity > ct
			&& upwardness < terrain_threshold
			? true : false;
	}
	ALBEDO = edge ? edge_color : ALBEDO;

	// literally just compares the depth of the current pixel and neighboring pixels
	ALBEDO = upwardness < terrain_threshold && (
		position[2].z - position[0].z > dt ||
		position[2].z - position[1].z > dt ||
		position[2].z - position[3].z > dt ||
		position[2].z - position[4].z > dt )
		? outline_color : ALBEDO;

	vec3 point = (INV_VIEW_MATRIX * vec4(VERTEX, 1.0)).xyz;
	float distortion = texture(color_noise, point).r;

	ALBEDO *= distortion;
}

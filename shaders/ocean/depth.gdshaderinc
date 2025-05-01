// godot's "proximity fade" but without reading depth for optimization

#define get_water_depth(dpt, dst) (_get_water_depth(dst, VERTEX, dpt, SCREEN_UV, INV_PROJECTION_MATRIX))

float _get_water_depth(float fade_dst, vec3 vertex, float depth, vec2 screen_uv, mat4 inv_proj_matrix) {
	vec4 world_pos = inv_proj_matrix * vec4(screen_uv * 2.0 - 1.0, depth, 1.0);
	world_pos.xyz /= world_pos.w;

	return clamp(1.0 - smoothstep(world_pos.z + fade_dst, world_pos.z, vertex.z), 0.0, 1.0);
}
shader_type spatial;

#include "depth.gdshaderinc"

uniform float depth_fade_distance = 1.0;
uniform float foam_fade_distance = 1.0;
uniform float depth_noise_factor = 1.0;
uniform float refraction_factor = 0.05;
uniform float fresnel_factor = 1.0;
uniform float ambient_wave_factor = 1.0;
uniform vec3 shallow_color : source_color;
uniform vec3 deep_color : source_color;
uniform vec3 fresnel_color : source_color;

uniform sampler2D depth_tex : hint_depth_texture;
uniform sampler2D depth_noise_tex;
uniform sampler2D water_noise_tex;
uniform sampler2D foam_noise_tex;

float get_water_height (vec2 position, float time) {
	float ambient = (texture(water_noise_tex, position * 0.002 + TIME / 60.0).r - 0.5) * ambient_wave_factor;
	return ambient;
}

void vertex() {
	vec3 pos = (MODEL_MATRIX * vec4(VERTEX, 1.0)).xyz;
	float height = get_water_height(pos.xz, TIME);
	VERTEX.y += height;
	NORMAL = normalize(vec3(height - get_water_height(pos.xz + vec2(0.1, 0.0), TIME), 0.1, height - get_water_height(pos.xz + vec2(0.0, 0.1), TIME)));
}

float get_ripple(float mask, float depth, float frequency, float offset, float thickness, float speed) {
    float wave = sin((depth + TIME * speed + offset) * frequency);
	float ambient = clamp((abs(wave) < thickness ? 1.0 : 0.0) * mask, 0.0, 1.0);

    return ambient;
}

void fragment() {
	vec3 world_coords = (INV_VIEW_MATRIX * vec4(VERTEX, 1.0)).xyz;
	vec2 refraction_noise = refraction_factor * (texture(water_noise_tex, world_coords.xz / 2.0 + TIME / 15.0).xy - 0.5);

	// gets correct depth to use as a mask for distortion
	// this may or may not be really stupid i didnt think this through
	float correct_screen_depth = texture(depth_tex, SCREEN_UV).r;
	float correct_depth = get_water_depth(correct_screen_depth, depth_fade_distance);

	float screen_depth = texture(depth_tex, SCREEN_UV + refraction_noise * 2.0 * (1.0 - correct_depth)).r;
	float depth = get_water_depth(screen_depth, depth_fade_distance);
	float faux_depth = 1.0 - dot(VIEW, NORMAL);
	float depth_noise = (texture(depth_noise_tex, (SCREEN_UV + TIME) / 40.0).r * depth_noise_factor + 0.5);

	vec3 base_albedo = mix(deep_color, shallow_color, depth_noise * depth * faux_depth);

	float foam_mask = 1.0 - smoothstep(0, foam_fade_distance, correct_depth);
	foam_mask = clamp(foam_mask, 0, 1);
	vec2 foam_noise = 0.02 * (texture(water_noise_tex, world_coords.xz / 2.0 + TIME / 15.0).xy - 0.5);
	foam_noise = clamp(foam_noise, 0, 1);
	float foam_ripple = texture(foam_noise_tex, world_coords.xz * 0.1 + TIME * 0.005).x * foam_mask + foam_mask * 0.3 - get_ripple(foam_mask, depth, 0.5, 0.3, 0.15, 0.0);
	foam_ripple = clamp(foam_ripple, 0, 1);
	foam_ripple = smoothstep(0, 1, foam_ripple);
	foam_ripple = 1.0 - step(foam_ripple, 0.5);


	float fresnel = pow(faux_depth, 12.0);

	vec3 final_albedo = clamp(mix(base_albedo + foam_ripple, fresnel_color * fresnel_factor, fresnel), 0.0, 1.0);

	ALBEDO = final_albedo;
	ALPHA = mix(1.0, 0.95, 1.0 - depth);
	ROUGHNESS = 0.2;
}
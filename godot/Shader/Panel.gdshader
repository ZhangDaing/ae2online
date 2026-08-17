shader_type canvas_item;

//2D纹理参数，当前帧的屏幕缓冲区(最终渲染结果)，禁止纹理重复，使用线性过滤和mipmap
uniform sampler2D screen_texture : hint_screen_texture,repeat_disable,filter_linear_mipmap;
//控制模糊的LOD级别
uniform float blur_lod : hint_range(0, 5.0) = 3.0;
//基础颜色，使用源颜色作为默认值
uniform vec4 base_color : source_color = vec4(0.0, 0.0, 0.0, 0.3);

void fragment() {
	vec3 blurred_bg = textureLod(screen_texture, SCREEN_UV, blur_lod).rgb;
	vec3 mixed = mix(blurred_bg, base_color.rgb, base_color.a);
	COLOR = vec4(mixed, 1.0);
}
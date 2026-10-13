#version 330
#moj_import <minecraft:dynamictransforms.glsl>
uniform sampler2D Sampler0;
in vec2 texCoord0;
in vec4 vertexColor;
out vec4 fragColor;
void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    if (color.a == 0.0) discard;
    float luminance = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
    fragColor = vec4(vec3(luminance * 0.70), color.a);
}

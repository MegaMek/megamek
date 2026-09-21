// Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later
// Shadow-map depth only; camera depth must retain the actual surface position.
#ifdef GL_ES
precision highp float;
#endif

#if defined(diffuseTextureFlag) && defined(blendedFlag)
varying vec2 v_texCoords0;
uniform sampler2D u_diffuseTexture;
uniform float u_alphaTest;
#endif

#ifdef PackedDepthFlag
varying float v_depth;
#endif

void main() {
#ifdef PackedDepthFlag
    // Measured in shadow texels, this follows the actual face slope, including vertical cliffs.
    // Half-texel PCF offsets plus nearest sampling reach at most one texel in each axis.
    // Evaluate derivatives before alpha discard so cutout edges retain valid neighbor values.
    float depth = min(1.0, v_depth + fwidth(v_depth) + 0.000001);
#endif
#if defined(diffuseTextureFlag) && defined(blendedFlag)
    if (texture2D(u_diffuseTexture, v_texCoords0).a < u_alphaTest) discard;
#endif
#ifdef PackedDepthFlag
    // Keep libGDX's packed RGBA encoding; all ground, prop and unit shaders read this same map.
    const vec4 carry = vec4(1.0 / 255.0, 1.0 / 255.0, 1.0 / 255.0, 0.0);
    vec4 color = vec4(depth, fract(depth * 255.0), fract(depth * 65025.0), fract(depth * 16581375.0));
    gl_FragColor = color - color.yzww * carry;
#endif
}

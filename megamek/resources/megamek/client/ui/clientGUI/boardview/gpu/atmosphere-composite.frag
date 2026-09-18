// Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later
#ifdef GL_ES
precision highp float;
#endif
varying vec2 v_uv;
uniform sampler2D u_scene;
uniform sampler2D u_depth;
uniform sampler2D u_fog;
uniform float u_fogEnabled;
uniform vec2 u_fogSize;
uniform float u_depthRange;
uniform float u_edgeScale;
uniform float u_exposure;
uniform float u_lightning;
uniform vec3 u_tint;
uniform float u_saturation;
uniform vec3 u_sky;
uniform vec3 u_horizon;

float depthAt(vec2 uv) {
    return dot(texture2D(u_depth, uv), vec4(1.0, 1.0 / 255.0, 1.0 / 65025.0, 1.0 / 16581375.0));
}

void main() {
    vec4 scene = texture2D(u_scene, v_uv);
    vec4 atmosphere = vec4(0, 0, 0, 1);
    if (u_fogEnabled > 0.5 && scene.a > 0.0) {
        float depth = depthAt(v_uv);
        // Bilateral upsampling keeps low-resolution fog from bleeding across roofs and unit silhouettes.
        vec2 pixel = v_uv * u_fogSize - 0.5;
        vec2 base = floor(pixel);
        vec2 f = fract(pixel);
        atmosphere = vec4(0.0);
        float weights = 0.0;
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 2; x++) {
                vec2 offset = vec2(float(x), float(y));
                vec2 uv = (base + offset + 0.5) / u_fogSize;
                vec2 bilinear = mix(1.0 - f, f, offset);
                float difference = abs(depth - depthAt(uv)) * u_depthRange;
                float weight = bilinear.x * bilinear.y / (1.0 + difference * difference / (u_edgeScale * u_edgeScale));
                atmosphere += texture2D(u_fog, uv) * weight;
                weights += weight;
            }
        }
        atmosphere = weights > 0.00001 ? atmosphere / weights : texture2D(u_fog, v_uv);
    }
    vec3 color = scene.rgb;
    if (scene.a <= 0.0) {
        color = mix(u_horizon * 0.55, u_sky, smoothstep(0.0, 1.0, v_uv.y));
    }
    vec3 linear = pow(max(color, vec3(0.0)), vec3(2.2));
    linear = (linear * atmosphere.a + atmosphere.rgb) * u_tint * u_exposure * (1.0 + u_lightning);
    float luminance = dot(linear, vec3(0.2126, 0.7152, 0.0722));
    linear = mix(vec3(luminance), linear, u_saturation);
    // The source artwork and scene target are LDR. An extra filmic curve would
    // amplify their baked contrast and destroy the tileset's original palette.
    linear = clamp(linear, 0.0, 1.0);
    color = pow(linear, vec3(1.0 / 2.2));
    vec2 edge = (v_uv - 0.5) * 2.0;
    color *= 1.0 - 0.09 * dot(edge, edge) * 0.5;
    gl_FragColor = vec4(color, 1.0);
}

// Copyright (C) 2026 The MegaMek Team.
// SPDX-License-Identifier: GPL-3.0-or-later

#ifdef GL_ES
precision mediump float;
#endif

uniform vec3 u_light;

varying vec2 v_uv;
varying float v_kind;
varying float v_fade;

void main() {
    bool rain = v_kind < 0.5;
    bool snow = v_kind > 0.5 && v_kind < 1.5;
    bool sand = v_kind > 2.5;

    float dist2 = dot(v_uv, v_uv);

    // Slightly crisper sand grains; other precipitation keeps the old softer shape.
    float shape = sand
        ? 1.0 - smoothstep(0.08, 0.85, dist2)
        : 1.0 - smoothstep(0.15, 1.0, dist2);

    float opacity = rain ? 0.64 : snow ? 0.72 : sand ? 1.0 : 0.65;

    vec3 color = rain
        ? vec3(0.62, 0.77, 0.91)
        : vec3(0.94, 0.97, 1.0);

    if (sand) {
        // Stronger contrast helps grains read over both pale and shaded terrain.
        float highlight = smoothstep(0.0, 0.75, v_uv.x + v_uv.y * 0.25);
        color = mix(vec3(0.28, 0.19, 0.08), vec3(0.98, 0.88, 0.68), highlight);
    }

    gl_FragColor = vec4(color * u_light, shape * opacity * v_fade);
}
// Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later
#ifdef GL_ES
precision mediump float;
#endif
varying vec2 v_uv;
varying vec2 v_effect;
void main() {
    if (v_effect.x < 0.5) {
        float lobes = 0.08 * sin(v_uv.x * 12.0) * sin(v_uv.y * 9.0);
        float shape = 1.0 - smoothstep(0.04, 1.0, dot(v_uv, v_uv) + lobes);
        vec3 smoke = mix(vec3(0.39, 0.43, 0.48), vec3(0.72, 0.75, 0.78), 0.5 + v_uv.y * 0.4);
        gl_FragColor = vec4(smoke, shape * v_effect.y);
    } else {
        float taper = 0.85 * (1.0 - v_uv.y) + 0.04;
        float radius = abs(v_uv.x) / taper;
        float edge = 1.0 - smoothstep(0.05, 1.0, radius);
        float tip = 1.0 - smoothstep(0.55, 1.0, v_uv.y);
        float core = (1.0 - smoothstep(0.0, 0.4, radius)) * (1.0 - smoothstep(0.1, 0.65, v_uv.y));
        vec3 blue = mix(vec3(0.025, 0.22, 1.0), vec3(0.08, 0.72, 1.0), edge);
        gl_FragColor = vec4(mix(blue, vec3(0.85, 0.96, 1.0), core), edge * tip * v_effect.y);
    }
}

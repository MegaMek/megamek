// Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later
#ifdef GL_ES
precision mediump float;
#endif
varying vec2 v_uv;
varying vec2 v_effect;
// Kind: 0 smoke, 1 blue jet flame, 2+ turbulent fire (integer packet seed plus fractional age).

float flameNoise(vec2 p) {
    vec2 cell = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    vec4 h = sin(vec4(dot(cell, vec2(127.1, 311.7)),
                     dot(cell + vec2(1.0, 0.0), vec2(127.1, 311.7)),
                     dot(cell + vec2(0.0, 1.0), vec2(127.1, 311.7)),
                     dot(cell + vec2(1.0, 1.0), vec2(127.1, 311.7)))) * 43758.5453;
    h = fract(h);
    return mix(mix(h.x, h.y, f.x), mix(h.z, h.w, f.x), f.y);
}

void main() {
    if (v_effect.x < 0.5) {
        float lobes = 0.08 * sin(v_uv.x * 12.0) * sin(v_uv.y * 9.0);
        float shape = 1.0 - smoothstep(0.04, 1.0, dot(v_uv, v_uv) + lobes);
        vec3 smoke = mix(vec3(0.39, 0.43, 0.48), vec3(0.72, 0.75, 0.78), 0.5 + v_uv.y * 0.4);
        gl_FragColor = vec4(smoke, shape * v_effect.y);
    } else if (v_effect.x > 1.5) {
        float age = fract(v_effect.x);
        float seed = floor(v_effect.x) * 7.13;
        vec2 p = v_uv;
        p.x += 0.14 * sin(p.y * 5.0 - age * 11.0 + seed);
        vec2 flow = p * vec2(3.1, 2.4) + vec2(seed, seed - age * 5.0);
        float noise = flameNoise(flow) * 0.65 + flameNoise(flow * 2.1 + seed) * 0.35;
        float radius = length(p * vec2(1.0, 0.86));
        float edge = 1.0 - smoothstep(0.40, 0.98, radius + (0.5 - noise) * 0.8);
        edge *= 1.0 - smoothstep(0.80, 1.0, max(abs(v_uv.x), abs(v_uv.y)));
        float heat = clamp(0.92 - age * 0.32 - radius * 0.43 + (noise - 0.5) * 0.55, 0.0, 1.0);
        vec3 flame = mix(vec3(0.85, 0.035, 0.002), vec3(1.0, 0.34, 0.015), smoothstep(0.12, 0.55, heat));
        flame = mix(flame, vec3(1.0, 0.88, 0.28), smoothstep(0.48, 0.86, heat));
        float core = (1.0 - smoothstep(0.0, 0.35, age)) * (1.0 - smoothstep(0.0, 0.38, radius));
        gl_FragColor = vec4(mix(flame, vec3(1.0, 0.97, 0.72), core), edge * v_effect.y);
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

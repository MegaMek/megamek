// Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later
#ifdef GL_ES
precision mediump float;
#endif
varying vec2 v_uv;
varying vec2 v_effect;
// Kind: 0 laser, 1 PPC beam/arc, 2 PPC charge/impact glow. Alpha is driven by the shared attack clock.

void main() {
    if (v_effect.x > 1.5) {
        float radius = length(v_uv);
        float core = 1.0 - smoothstep(0.08, 0.32, radius);
        float halo = exp(-radius * radius * 5.0) * (1.0 - smoothstep(0.7, 1.0, radius));
        vec3 color = mix(vec3(0.08, 0.45, 1.0), vec3(0.92, 0.98, 1.0), core);
        gl_FragColor = vec4(color, max(core, halo * 0.8) * v_effect.y);
    } else if (v_effect.x > 0.5) {
        float radius = abs(v_uv.x);
        float core = 1.0 - smoothstep(0.08, 0.28, radius);
        float halo = exp(-radius * radius * 6.0) * (1.0 - smoothstep(0.7, 1.0, radius));
        vec3 color = mix(vec3(0.06, 0.38, 1.0), vec3(0.92, 0.98, 1.0), core);
        gl_FragColor = vec4(color, max(core, halo * 0.7) * v_effect.y);
    } else {
        float edge = 1.0 - smoothstep(0.25, 1.0, abs(v_uv.x));
        float core = 1.0 - smoothstep(0.0, 0.4, abs(v_uv.x));
        gl_FragColor = vec4(mix(vec3(1.0, 0.015, 0.005), vec3(1.0, 0.65, 0.45), core), edge * v_effect.y);
    }
}

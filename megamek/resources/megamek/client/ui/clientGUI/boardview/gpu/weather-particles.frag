// Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later
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
    float shape = 1.0 - smoothstep(0.15, 1.0, dot(v_uv, v_uv));
    float opacity = rain ? 0.64 : snow ? 0.72 : sand ? 0.68 : 0.65;
    vec3 color = sand ? vec3(0.76, 0.66, 0.48) : rain ? vec3(0.62, 0.77, 0.91) : vec3(0.94, 0.97, 1.0);
    gl_FragColor = vec4(color * u_light, shape * opacity * v_fade);
}

// Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later
#ifdef GL_ES
precision highp float;
#endif
varying vec2 v_uv;
uniform sampler2D u_sceneDepth;
uniform sampler2D u_unitDepth;
uniform sampler2D u_unitColors;
uniform vec2 u_step;
uniform float u_bias;
uniform float u_intensity;

float depthAt(sampler2D map, vec2 uv) {
    return texture2D(map, uv).r;
}

float hiddenAt(vec2 uv) {
    if (min(uv.x, uv.y) < 0.0 || max(uv.x, uv.y) > 1.0) return 0.0;
    float unit = depthAt(u_unitDepth, uv);
    return (1.0 - step(0.99999, unit)) * step(texture2D(u_sceneDepth, uv).r + u_bias, unit);
}

void main() {
    float unit = depthAt(u_unitDepth, v_uv);
    float hidden = hiddenAt(v_uv);
    // Neither fill nor halo may repaint the normally visible part of a unit.
    if (unit < 0.99999 && hidden < 0.5) discard;

    float nearMax = hidden;
    float nearMin = hidden;
    float farMax = hidden;
    vec2 colorUV = v_uv;
    float nearest = hidden > 0.5 ? 0.0 : 10.0;
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            if (x == 0 && y == 0) continue;
            vec2 offset = vec2(float(x), float(y)) * u_step;
            float nearby = hiddenAt(v_uv + offset);
            float distance = float(x * x + y * y);
            if (nearby > 0.5 && distance < nearest) {
                nearest = distance;
                colorUV = v_uv + offset;
            }
            nearMax = max(nearMax, nearby);
            nearMin = min(nearMin, nearby);
            farMax = max(farMax, hiddenAt(v_uv + offset * 2.0));
        }
    }
    float edge = nearMax - nearMin;
    if (nearMax > 0.5) {
        // Both the edge and faint interior follow the unit's team/player color.
        gl_FragColor = vec4(texture2D(u_unitColors, colorUV).rgb,
              u_intensity * mix(0.24, 1.0, edge));
    } else if (farMax > 0.5) {
        // A narrow dark halo retains contrast against snow, water and bright terrain artwork.
        gl_FragColor = vec4(0.025, 0.055, 0.07, u_intensity * 0.65);
    } else {
        discard;
    }
}

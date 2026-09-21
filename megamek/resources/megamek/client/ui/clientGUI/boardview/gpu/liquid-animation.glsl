// Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later
// Animate the authored shapes in place. Only real waterfall faces receive scrolling UVs from the material.
uniform sampler2D u_liquidNextFrame;
uniform float u_liquidBlend;

vec4 liquidSample(vec2 uv) {
    return mix(texture2D(u_diffuseTexture, uv), texture2D(u_liquidNextFrame, uv), u_liquidBlend);
}

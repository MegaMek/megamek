// Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later
float sunDepth(vec2 uv) {
    #ifdef VERTEX_SUN_VISIBILITY
        return texture2DLod(u_depth, uv, 0.0).r;
    #else
        return texture2D(u_depth, uv).r;
    #endif
}

// Every vertex sees the same source footprint, so interpolation preserves partial occlusion.
// Clamped depth at the viewport border also suppresses offscreen glow behind edge geometry.
float sunVisibility() {
    vec2 footprint = vec2(0.008 / u_sunGlare.w, 0.008);
    float visible = step(0.99999, sunDepth(u_sunGlare.xy)) * 0.4;
    visible += step(0.99999, sunDepth(u_sunGlare.xy + vec2(footprint.x, 0.0))) * 0.15;
    visible += step(0.99999, sunDepth(u_sunGlare.xy - vec2(footprint.x, 0.0))) * 0.15;
    visible += step(0.99999, sunDepth(u_sunGlare.xy + vec2(0.0, footprint.y))) * 0.15;
    visible += step(0.99999, sunDepth(u_sunGlare.xy - vec2(0.0, footprint.y))) * 0.15;
    return visible;
}

// Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later
// Frozen pre-optimization glare for paired GPU timing and image comparison; never shipped by the renderer.
vec3 sunGlare() {
    if (u_sunGlare.z <= 0.0) return vec3(0.0);
    vec2 offset = (v_uv - u_sunGlare.xy) * vec2(u_sunGlare.w, 1.0);
    float radius = dot(offset, offset);
    vec2 footprint = vec2(0.008 / u_sunGlare.w, 0.008);
    float visible = step(0.99999, depthAt(u_sunGlare.xy)) * 0.4;
    visible += step(0.99999, depthAt(u_sunGlare.xy + vec2(footprint.x, 0.0))) * 0.15;
    visible += step(0.99999, depthAt(u_sunGlare.xy - vec2(footprint.x, 0.0))) * 0.15;
    visible += step(0.99999, depthAt(u_sunGlare.xy + vec2(0.0, footprint.y))) * 0.15;
    visible += step(0.99999, depthAt(u_sunGlare.xy - vec2(0.0, footprint.y))) * 0.15;
    if (visible <= 0.0) return vec3(0.0);
    vec3 gold = mix(u_glareColor, vec3(1.0, 0.62, 0.13), 0.6);
    vec3 hot = mix(u_glareColor, vec3(1.0, 0.98, 0.9), 0.9);
    vec3 flare = hot * (2.8 * exp2(-radius * 380.0));
    flare += gold * (1.2 * exp2(-radius * 14.0) + 0.24 * exp2(-radius * 1.6));
    flare += hot * (0.08 * exp2(-abs(offset.x) * 10.0 - abs(offset.y) * 170.0));
    vec2 point = (v_uv - 0.5) * vec2(u_sunGlare.w, 1.0);
    vec2 source = (u_sunGlare.xy - 0.5) * vec2(u_sunGlare.w, 1.0);
    vec2 smallGhost = point + source * 0.35;
    float disc = 1.0 - smoothstep(0.018, 0.035, length(smallGhost));
    flare += vec3(1.0, 0.78, 0.18) * (0.18 * disc);
    vec2 largeGhost = point + source * 0.85;
    float distance = length(largeGhost);
    flare += vec3(1.0, 0.72, 0.50) * (0.40 * exp2(-distance * distance * 1600.0));
    flare += vec3(1.0, 0.42, 0.16) * (0.10 * (1.0 - smoothstep(0.032, 0.052, distance)));
    vec2 axis = source / max(length(source), 0.001);
    float crescent = smoothstep(-0.4, 0.7, -dot(largeGhost, axis) / max(distance, 0.001));
    float ring = (distance - 0.115) / 0.018;
    vec3 rim = mix(vec3(0.85, 0.72, 0.22), vec3(1.0, 0.26, 0.06), smoothstep(0.10, 0.13, distance));
    flare += rim * (0.24 * exp2(-ring * ring) * crescent);
    return flare * (u_sunGlare.z * visible);
}

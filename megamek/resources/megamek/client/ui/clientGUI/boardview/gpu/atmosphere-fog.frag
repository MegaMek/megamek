// Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later
#ifdef GL_ES
#extension GL_OES_standard_derivatives : enable
precision highp float;
#endif
varying vec2 v_uv;
uniform sampler2D u_depth;
uniform vec3 u_fog;
uniform float u_haze;
uniform float u_noiseScale;
uniform vec3 u_fogVariation; // Height delta, density thinning, minimum local height.
uniform vec2 u_fogOffset;
uniform vec3 u_fogColor;
uniform float u_maxOpacity;
uniform float u_rays;
uniform vec3 u_sunColor;
uniform vec3 u_sunDirection;
uniform sampler2D u_geometryShadow;
uniform mat4 u_geometryProjection;
uniform float u_hasGeometryShadow;
#define cloudShadowFlag
#define cloudVolumeFlag
// CLOUD_SHADOW
// SCATTERING_PHASE
// GROUND_LAYER

float depthAt(sampler2D map, vec2 uv) {
    return dot(texture2D(map, uv), vec4(1.0, 1.0 / 255.0, 1.0 / 65025.0, 1.0 / 16581375.0));
}

float sunlight(vec3 position) {
    float light = cloudTransmission(position);
    if (u_hasGeometryShadow > 0.5) {
        vec4 projected = u_geometryProjection * vec4(position, 1.0);
        vec3 uv = projected.xyz / projected.w * 0.5 + 0.5;
        if (uv.x > 0.0 && uv.y > 0.0 && uv.x < 1.0 && uv.y < 1.0 && uv.z > 0.0 && uv.z < 1.0) {
            light *= step(uv.z - 0.0005, depthAt(u_geometryShadow, uv.xy));
        }
    }
    return light;
}

void main() {
    if (outsideGroundLayer()) {
        gl_FragColor = vec4(0, 0, 0, 1);
        return;
    }
    vec3 origin = world(0.0);
    float surface = texture2D(u_depth, v_uv).r;
    // Fog occupies air above the board, including against sky, while leaving the solid plinth untouched.
    vec3 endpoint = world(surface);
    if (groundBaseSide(endpoint, surface, u_fog.z, u_fog.y * 0.001)) {
        gl_FragColor = vec4(0, 0, 0, 1);
        return;
    }
    vec2 segment = groundSegment(origin, endpoint);
    float start = segment.x, end = segment.y;
    if (end <= start || u_fog.x + u_haze + u_rays <= 0.0) {
        gl_FragColor = vec4(0, 0, 0, 1);
        return;
    }
    // Keep fog/haze sampling independent of the taller cloud-shaft volume.
    float inverseZ = 1.0 / min(u_direction.z, -0.000001);
    float top = max((u_fog.y + u_fogVariation.x) * 3.0, u_haze > 0.0 ? u_fog.y * 6.0 : 0.0);
    float fogStart = clamp((u_fog.z + top - origin.z) * inverseZ, start, end);
    float stepLength = (end - fogStart) * 0.25;
    float opticalDepth = 0.0;
    for (int i = 0; i < 4; i++) {
        float from = fogStart + float(i) * stepLength;
        vec3 first = origin + u_direction * from;
        vec3 last = first + u_direction * stepLength;
        vec3 position = (first + last) * 0.5;
        float height = u_fog.y, density = 1.0;
        if (u_fog.x > 0.0 && u_fogVariation.x + u_fogVariation.y > 0.0) {
            vec3 point = vec3(position.xy * u_noiseScale - u_fogOffset,
                  (position.z - u_fog.z) / u_fog.y);
            float bank = smoothstep(0.22, 0.78, groundNoise(point) * 0.7 + groundNoise(point * 2.0) * 0.3);
            height = max(u_fogVariation.z, height + (bank * 2.0 - 1.0) * u_fogVariation.x);
            density = mix(1.0, bank * bank * bank * bank, u_fogVariation.y);
        }
        density *= 1.0 - smoothstep(height * 1.5, height * 3.0, position.z - u_fog.z);
        float integral = heightIntegral(first.z - u_fog.z, last.z - u_fog.z, stepLength, height);
        float hazeStart = clamp((u_fog.z + u_fog.y * 6.0 - origin.z) * inverseZ, from, from + stepLength);
        float edge = groundEdge(position, 0.5 / u_noiseScale);
        opticalDepth += (u_fog.x * integral * density + u_haze * (from + stepLength - hazeStart)) * edge;
    }
    float opacity = min(u_maxOpacity, 1.0 - exp(-opticalDepth));
    vec3 illumination = pow(u_fogColor, vec3(2.2));
    // Fog and haze share one opacity ceiling, even when both controls are at their maximum.
    vec3 scattered = illumination * opacity;
    // At the opacity cap the shaft contribution is exactly zero: skip all twelve shadow samples.
    if (u_rays > 0.0 && opacity < u_maxOpacity && surface < 0.99999) {
        float lit = 0.0;
        float transmission = 1.0;
        float stepLength = (end - start) / 12.0;
        for (int i = 0; i < 12; i++) {
            vec3 position = origin + u_direction * (start + (float(i) + 0.5) * stepLength);
            float density = u_rays * (0.35 + 0.65 * exp(-max(0.0, position.z - u_fog.z) / (u_fog.y * 4.0)));
            density *= groundEdge(position, 0.5 / u_noiseScale);
            float segment = 1.0 - exp(-density * stepLength);
            lit += transmission * segment * sunlight(position);
            transmission *= 1.0 - segment;
        }
        float shaftOpacity = min(u_maxOpacity - opacity, (1.0 - opacity) * (1.0 - transmission));
        float phase = scatteringPhase(dot(u_direction, u_sunDirection), 0.5);
        vec3 shaftLight = illumination * 0.05 + u_sunColor * (lit / max(0.00001, 1.0 - transmission)) * phase * 2.0;
        scattered += shaftLight * shaftOpacity;
        opacity += shaftOpacity;
    }
    gl_FragColor = vec4(scattered, 1.0 - opacity);
}

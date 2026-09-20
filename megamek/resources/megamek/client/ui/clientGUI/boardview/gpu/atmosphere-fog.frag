// Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later
#ifdef GL_ES
precision highp float;
#endif
varying vec2 v_uv;
uniform sampler2D u_depth;
uniform mat4 u_inverseView;
uniform vec3 u_direction;
uniform vec3 u_boundsMin;
uniform vec3 u_boundsMax;
uniform vec3 u_fog;
uniform float u_haze;
uniform float u_noiseScale;
uniform float u_clock;
uniform vec3 u_fogColor;
uniform float u_maxOpacity;

float depthAt(sampler2D map, vec2 uv) {
    return dot(texture2D(map, uv), vec4(1.0, 1.0 / 255.0, 1.0 / 65025.0, 1.0 / 16581375.0));
}

vec3 world(float depth) {
    vec4 p = u_inverseView * vec4(v_uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return p.xyz / p.w;
}

void main() {
    vec3 origin = world(0.0);
    float surface = min(depthAt(u_depth, v_uv), 1.0);
    // The backdrop supplies sky scattering; do not expose a finite fog box behind the board silhouette.
    if (surface >= 0.99999) {
        gl_FragColor = vec4(0, 0, 0, 1);
        return;
    }
    float distanceToSurface = dot(world(surface) - origin, u_direction);
    vec3 inverseRay = 1.0 / (u_direction + vec3(0.000001));
    vec3 a = (u_boundsMin - origin) * inverseRay;
    vec3 b = (u_boundsMax - origin) * inverseRay;
    vec3 nearBox = min(a, b);
    vec3 farBox = max(a, b);
    float start = max(0.0, max(nearBox.x, max(nearBox.y, nearBox.z)));
    float end = min(distanceToSurface, min(farBox.x, min(farBox.y, farBox.z)));
    if (end <= start || u_fog.x + u_haze <= 0.0) {
        gl_FragColor = vec4(0, 0, 0, 1);
        return;
    }
    vec3 first = origin + u_direction * start;
    vec3 last = origin + u_direction * end;
    float z0 = first.z - u_fog.z;
    float z1 = last.z - u_fog.z;
    // Integrate exponential height density directly instead of marching noise and shadows 24 times.
    float integral = (u_fog.y * abs(exp(-max(0.0, z1) / u_fog.y) - exp(-max(0.0, z0) / u_fog.y))
          + abs(min(z1, 0.0) - min(z0, 0.0))) / max(abs(u_direction.z), 0.001);
    if (abs(u_direction.z) < 0.001) integral = (end - start) * exp(-max(0.0, z1) / u_fog.y);
    float wisps = 0.9 + 0.1 * sin(last.x * u_noiseScale + u_clock * 0.06)
          * sin(last.y * u_noiseScale - u_clock * 0.04);
    float opacity = min(u_maxOpacity, 1.0 - exp(-u_fog.x * integral * wisps - u_haze * (end - start)));
    vec3 illumination = pow(u_fogColor, vec3(2.2));
    // Fog and haze share one opacity ceiling, even when both controls are at their maximum.
    gl_FragColor = vec4(illumination * opacity, 1.0 - opacity);
}

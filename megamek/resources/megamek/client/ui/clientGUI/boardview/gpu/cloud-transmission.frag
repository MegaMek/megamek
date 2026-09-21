// Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later
#ifdef GL_ES
precision highp float;
#endif
varying vec2 v_uv;
uniform vec4 u_cloudBounds;
uniform sampler2D u_cloudNoise;
uniform vec4 u_cloudWeather; // coverage, stratus fraction, extinction per world unit, world-to-noise scale
uniform float u_shadowStrength; // cover-dependent opacity cap, without changing cloud density or openings
uniform vec2 u_cloudLayer; // base, thickness
uniform vec3 u_cloudOffset; // integrated horizontal advection and slow evolution, in noise cells
uniform vec3 u_sunDirection;

float cloudNoise(vec3 p) {
    vec3 cell = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    vec2 uv = cell.xy + vec2(37.0, 17.0) * cell.z + f.xy;
    vec2 pair = texture2D(u_cloudNoise, (uv + 0.5) / 256.0).rg;
    return mix(pair.x, pair.y, f.z);
}

float cloudDensity(vec3 position) {
    float h = (position.z - u_cloudLayer.x) / u_cloudLayer.y;
    if (h <= 0.0 || h >= 1.0) return 0.0;
    vec3 p = vec3(position.xy * u_cloudWeather.w - u_cloudOffset.xy,
          h * mix(2.8, 0.6, u_cloudWeather.y) + u_cloudOffset.z);
    // Horizontal coverage keeps openings through the full layer.
    float weather = cloudNoise(vec3(p.xy, 21.0)) * 0.7 + cloudNoise(vec3(p.xy * 2.0, 42.0)) * 0.3;
    float threshold = mix(0.85, 0.10, u_cloudWeather.x);
    float coverage = smoothstep(threshold, threshold + 0.08, weather);
    if (coverage <= 0.0) return 0.0;
    // Integer octaves retain the 256-cell period, including negative wind wrapping.
    float detail = cloudNoise(p * 8.0);
    float shape = cloudNoise(p * 2.0) * 0.65 + cloudNoise(p * 4.0) * 0.25 + detail * 0.10;
    float body = clamp((shape - mix(0.52, 0.32, coverage)) / 0.30, 0.0, 1.0);
    body = max(0.0, body - (1.0 - body) * (1.0 - detail) * 0.5);
    float top = mix(0.45 + shape * 0.55, 0.95, u_cloudWeather.y);
    float profile = smoothstep(0.0, 0.08, h) * (1.0 - smoothstep(top * 0.75, top, h));
    return coverage * body * profile;
}

void main() {
    vec3 origin = vec3(u_cloudBounds.xy + v_uv * u_cloudBounds.zw, u_cloudLayer.x);
    float stepLength = u_cloudLayer.y / max(u_sunDirection.z, 0.1) / 12.0;
    float opticalDepth = 0.0;
    for (int i = 0; i < 12; i++) {
        vec3 position = origin + u_sunDirection * (float(i) + 0.5) * stepLength;
        opticalDepth += cloudDensity(position) * stepLength * u_cloudWeather.z;
    }
    float transmission = mix(1.0, exp(-opticalDepth), u_shadowStrength);
    gl_FragColor = vec4(transmission, transmission, transmission, 1.0);
}

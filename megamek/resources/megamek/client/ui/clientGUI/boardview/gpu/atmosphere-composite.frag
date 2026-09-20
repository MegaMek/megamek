// Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later
#ifdef GL_ES
#extension GL_OES_standard_derivatives : enable
precision highp float;
#endif
varying vec2 v_uv;
uniform sampler2D u_scene;
uniform sampler2D u_depth;
uniform sampler2D u_fog;
uniform float u_fogEnabled;
uniform vec2 u_fogSize;
uniform float u_depthRange;
uniform float u_edgeScale;
uniform float u_exposure;
uniform float u_lightning;
uniform vec3 u_tint;
uniform float u_saturation;
uniform vec3 u_sky;
uniform vec3 u_horizon;
// FoV is a rules-derived mask applied to reconstructed world positions, never a repaint of terrain artwork.
uniform sampler2D u_fov;
uniform float u_fovEnabled;
uniform vec2 u_fovSize;
uniform vec2 u_fovHexSize;
uniform mat4 u_fovInverseView;
uniform vec2 u_fovOptions; // Distance-ring opacity, spotting tint.
uniform vec3 u_fovEffect; // Opacity, grayscale, fog-of-war for hexes outside visual LOS.
uniform vec3 u_sensorEffect; // Same settings for hexes outside visual and sensor coverage.
uniform float u_dimmedDesaturation;
uniform float u_fovEdge;

vec2 fovCenter(vec2 hex) {
    return vec2(hex.x * 0.75 + 0.5, hex.y + mod(hex.x, 2.0) * 0.5 + 0.5);
}

vec2 fovHex(vec2 point) {
    float column = floor(point.x / 0.75) - 1.0;
    vec2 hex = vec2(column, floor(point.y - mod(column, 2.0) * 0.5));
    vec2 delta = abs(point - fovCenter(hex));
    if (delta.y <= 0.5 && delta.x + 0.5 * delta.y <= 0.5) return hex;
    column += 1.0;
    return vec2(column, floor(point.y - mod(column, 2.0) * 0.5));
}

vec4 fovAt(vec2 hex) {
    if (hex.x < 0.0 || hex.y < 0.0 || hex.x >= u_fovSize.x || hex.y >= u_fovSize.y) return vec4(0.0);
    return texture2D(u_fov, (hex + 0.5) / u_fovSize);
}

float fovState(vec4 value) {
    return mod(floor(value.a * 255.0 + 0.5), 8.0);
}

vec3 fovEffect(vec4 mask) {
    // BLOCKED also includes ordinary LOS obstructions and previews without a selected sensor.
    return mask.a * 255.0 >= 15.5 ? u_sensorEffect : u_fovEffect;
}

float fovBorder(vec2 neighbor, float distance, vec4 mask) {
    float state = fovState(mask);
    vec4 otherMask = fovAt(neighbor);
    float other = fovState(otherMask);
    if (other < 0.5 || (state < 2.5) == (other < 2.5)) return 0.0;
    float opacity = fovEffect(state > 2.5 ? mask : otherMask).x;
    if (opacity <= 0.0) return 0.0;
    return 1.0 - smoothstep(u_fovEdge * 0.5, u_fovEdge * 2.0, distance);
}

vec3 fieldOfView(vec3 color, float depth) {
    vec4 world = u_fovInverseView * vec4(v_uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec3 position = world.xyz / world.w;
    vec3 normal = cross(dFdx(position), dFdy(position));
    normal /= max(length(normal), 0.000001);
    // Derivatives need the whole pixel quad, including background lanes at silhouettes.
    if (depth >= 0.99999) return color;
    float horizontal = smoothstep(0.35, 0.80, abs(normal.z));
    vec2 point = vec2(position.x, -position.y) / u_fovHexSize;
    // A cliff sits exactly on a shared edge. Reconstructed depth can round to either
    // hex, so sample just inside its solid side instead of letting the wall sparkle.
    // Top faces have no horizontal normal and keep their original lookup position.
    vec3 inside = position - normal * (min(u_fovHexSize.x, u_fovHexSize.y) * 0.002);
    vec2 hex = fovHex(vec2(inside.x, -inside.y) / u_fovHexSize);
    vec4 mask = fovAt(hex);
    float state = fovState(mask);
    if (state < 0.5) return color;
    vec2 p = point - fovCenter(hex);
    float north = 0.5 + p.y, south = 0.5 - p.y;
    float northEast = 0.5 - p.x + 0.5 * p.y, southEast = 0.5 - p.x - 0.5 * p.y;
    float northWest = 0.5 + p.x + 0.5 * p.y, southWest = 0.5 + p.x - 0.5 * p.y;
    float parity = mod(hex.x, 2.0);
    float boundary = fovBorder(hex + vec2(0, -1), north, mask);
    boundary = max(boundary, fovBorder(hex + vec2(1, parity - 1.0), northEast, mask));
    boundary = max(boundary, fovBorder(hex + vec2(1, parity), southEast, mask));
    boundary = max(boundary, fovBorder(hex + vec2(0, 1), south, mask));
    boundary = max(boundary, fovBorder(hex + vec2(-1, parity), southWest, mask));
    boundary = max(boundary, fovBorder(hex + vec2(-1, parity - 1.0), northWest, mask));
    vec3 effect = fovEffect(mask);
    if (state > 2.5) {
        bool sensor = state < 3.5;
        float amount = effect.x;
        float gray = dot(color, vec3(0.2126, 0.7152, 0.0722));
        float desaturate = amount > 0.0 ? (effect.z > 0.5 ? 0.85 : u_dimmedDesaturation) : 0.0;
        color = mix(color, vec3(gray), desaturate);
        vec3 shade = sensor ? vec3(0.10, 0.20, 0.25) : vec3(0.025, 0.035, 0.055);
        if (u_fovOptions.y > 0.5) shade.b += 0.10;
        if (effect.z > 0.5) amount = 1.0 - pow(1.0 - amount, 4.0);
        color = mix(color, shade, amount);
    } else if (state < 1.5 && mod(floor(mask.a * 255.0 + 0.5), 16.0) > 8.0) {
        color = mix(color, mask.rgb, u_fovOptions.x);
    }
    if (state > 1.5 && state < 2.5) {
        float edge = min(min(north, south), min(min(northEast, southEast), min(northWest, southWest)));
        color = mix(color, mask.rgb, (1.0 - smoothstep(0.015, 0.04, edge)) * 0.28 * horizontal);
    }
    // A contour appears only where visible hexes meet an enabled blocked/sensor effect.
    color = mix(color, vec3(0.40, 0.78, 0.84), boundary * 0.60 * horizontal);
    if (state > 2.5 && effect.y > 0.5) {
        // Desaturate last so sensor/spotting tints and the contour remain grayscale too.
        color = vec3(dot(color, vec3(0.2126, 0.7152, 0.0722)));
    }
    return color;
}


float depthAt(vec2 uv) {
    return dot(texture2D(u_depth, uv), vec4(1.0, 1.0 / 255.0, 1.0 / 65025.0, 1.0 / 16581375.0));
}

void main() {
    vec4 scene = texture2D(u_scene, v_uv);
    vec4 atmosphere = vec4(0, 0, 0, 1);
    if (u_fogEnabled > 0.5 && scene.a > 0.0) {
        float depth = depthAt(v_uv);
        // Bilateral upsampling keeps low-resolution fog from bleeding across roofs and unit silhouettes.
        vec2 pixel = v_uv * u_fogSize - 0.5;
        vec2 base = floor(pixel);
        vec2 f = fract(pixel);
        atmosphere = vec4(0.0);
        float weights = 0.0;
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 2; x++) {
                vec2 offset = vec2(float(x), float(y));
                vec2 uv = (base + offset + 0.5) / u_fogSize;
                vec2 bilinear = mix(1.0 - f, f, offset);
                float difference = abs(depth - depthAt(uv)) * u_depthRange;
                float weight = bilinear.x * bilinear.y / (1.0 + difference * difference / (u_edgeScale * u_edgeScale));
                atmosphere += texture2D(u_fog, uv) * weight;
                weights += weight;
            }
        }
        atmosphere = weights > 0.00001 ? atmosphere / weights : texture2D(u_fog, v_uv);
    }
    vec3 color = scene.rgb;
    if (scene.a <= 0.0) {
        color = mix(u_horizon * 0.55, u_sky, smoothstep(0.0, 1.0, v_uv.y));
    }
    vec3 linear = pow(max(color, vec3(0.0)), vec3(2.2));
    linear = (linear * atmosphere.a + atmosphere.rgb) * u_tint * u_exposure * (1.0 + u_lightning);
    float luminance = dot(linear, vec3(0.2126, 0.7152, 0.0722));
    linear = mix(vec3(luminance), linear, u_saturation);
    // The source artwork and scene target are LDR. An extra filmic curve would
    // amplify their baked contrast and destroy the tileset's original palette.
    linear = clamp(linear, 0.0, 1.0);
    color = pow(linear, vec3(1.0 / 2.2));
    vec2 edge = (v_uv - 0.5) * 2.0;
    color *= 1.0 - 0.09 * dot(edge, edge) * 0.5;
    if (u_fovEnabled > 0.5) color = fieldOfView(color, depthAt(v_uv));
    gl_FragColor = vec4(color, 1.0);
}

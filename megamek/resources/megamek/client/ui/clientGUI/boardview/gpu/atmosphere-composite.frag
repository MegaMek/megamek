// Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later
#ifdef GL_ES
#extension GL_OES_standard_derivatives : enable
#extension GL_EXT_frag_depth : require
precision highp float;
#define DEPTH gl_FragDepthEXT
#else
#define DEPTH gl_FragDepth
#endif
varying vec2 v_uv;
uniform sampler2D u_scene;
uniform sampler2D u_depth;
uniform sampler2D u_fog;
uniform float u_fogEnabled;
uniform vec4 u_scatteringBounds;
uniform vec2 u_fogSize;
uniform float u_depthRange;
uniform float u_edgeScale;
uniform float u_exposure;
uniform float u_lightning;
uniform vec3 u_tint;
uniform float u_saturation;
uniform vec3 u_sky;
uniform vec3 u_horizon;
uniform vec4 u_sunGlare; // Angular screen position, strength, viewport aspect.
uniform vec3 u_glareColor;
#ifdef VERTEX_SUN_VISIBILITY
varying float v_sunVisibility;
#else
// SUN_VISIBILITY
#endif
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
uniform vec4 u_sand; // Strength, height, board baseline, inverse terrain-level height.
uniform vec4 u_sandWind; // Unit direction, integrated fine-grain travel, inverse hex width.
uniform vec2 u_sandOffset;
uniform vec3 u_sandLight;
uniform float u_sandMaxOpacity;
// GROUND_LAYER

vec4 sandLayer(float depth) {
    if (u_sand.x <= 0.0 || outsideGroundLayer()) return vec4(0.0, 0.0, 0.0, 1.0);
    vec3 origin = world(0.0), surface = world(depth);
    if (groundBaseSide(surface, depth, u_sand.z, 0.01 / u_sand.w)) return vec4(0.0, 0.0, 0.0, 1.0);
    vec2 segment = groundSegment(origin, surface);
    if (segment.y <= segment.x) return vec4(0.0, 0.0, 0.0, 1.0);
    vec3 first = origin + u_direction * segment.x;
    vec3 last = origin + u_direction * segment.y;
    float z0 = max(0.0, first.z - u_sand.z), z1 = max(0.0, last.z - u_sand.z);
    float nearDensity = exp(-z0 / u_sand.y), farDensity = exp(-z1 / u_sand.y);
    float difference = farDensity - nearDensity;
    float integral = u_sand.y * difference / max(abs(u_direction.z), 0.001);
    // Sample the density-weighted centre of the actual air column, rather than projecting noise onto the ground.
    float meanHeight = abs(difference) > 0.00001
          ? ((z1 + u_sand.y) * farDensity - (z0 + u_sand.y) * nearDensity) / difference : (z0 + z1) * 0.5;
    float distance = clamp((u_sand.z + clamp(meanHeight, z1, z0) - origin.z) / min(u_direction.z, -0.000001),
          segment.x, segment.y);
    vec3 position = origin + u_direction * distance;
    float altitude = max(0.0, position.z - u_sand.z);
    vec3 field = vec3(position.xy * (u_sandWind.w * 0.5) - u_sandOffset, altitude / u_sand.y);
    float gust = smoothstep(0.2, 0.8, groundNoise(field));
    vec2 crosswind = vec2(-u_sandWind.y, u_sandWind.x);
    vec3 grains = vec3(dot(position.xy, u_sandWind.xy) * u_sandWind.w * 24.0 - u_sandWind.z,
          dot(position.xy, crosswind) * u_sandWind.w * 72.0, altitude * u_sandWind.w * 32.0);
    // Filter with the actual 3D footprint. Mips of the packed Z-slice atlas would create seams.
    float footprint = max(length(dFdx(grains)), length(dFdy(grains)));
    float grain = mix(groundNoise(grains), 0.5, smoothstep(0.5, 1.5, footprint));
    float density = (0.25 + 0.75 * gust) * (0.55 + 0.7 * grain);
    density *= 1.0 - smoothstep(u_sand.y * 1.5, u_sand.y * 3.0, altitude);
    density *= groundEdge(position, 1.0 / u_sandWind.w);
    float opacity = min(u_sandMaxOpacity, 1.0 - exp(-u_sand.x * integral * u_sand.w * density * 0.12));
    vec3 dust = mix(vec3(0.46, 0.29, 0.12), vec3(0.68, 0.47, 0.22), grain) * u_sandLight;
    return vec4(dust * opacity, 1.0 - opacity);
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
    vec2 hex = boardHex(vec2(inside.x, -inside.y) / u_fovHexSize);
    vec4 mask = fovAt(hex);
    float state = fovState(mask);
    if (state < 0.5) return color;
    vec2 p = point - boardHexCenter(hex);
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
    return texture2D(u_depth, uv).r;
}

vec3 sunGlare() {
    if (u_sunGlare.z <= 0.0) return vec3(0.0);
    vec2 offset = (v_uv - u_sunGlare.xy) * vec2(u_sunGlare.w, 1.0);
    float radius = dot(offset, offset);
    #ifdef VERTEX_SUN_VISIBILITY
        float visible = v_sunVisibility;
    #else
        float visible = sunVisibility();
    #endif
    if (visible <= 0.0) return vec3(0.0);
    vec3 gold = mix(u_glareColor, vec3(1.0, 0.62, 0.13), 0.6);
    vec3 hot = mix(u_glareColor, vec3(1.0, 0.98, 0.9), 0.9);
    // Bright source bloom and broad veiling glare: add warm light over the scene without blurring its details.
    vec3 flare = hot * (2.8 * exp2(-radius * 380.0));
    flare += gold * (1.2 * exp2(-radius * 14.0) + 0.24 * exp2(-radius * 1.6));
    flare += hot * (0.08 * exp2(-abs(offset.x) * 10.0 - abs(offset.y) * 170.0));
    // Lens reflections follow the source-to-optical-center axis, on the opposite side of the image.
    vec2 point = (v_uv - 0.5) * vec2(u_sunGlare.w, 1.0);
    vec2 source = (u_sunGlare.xy - 0.5) * vec2(u_sunGlare.w, 1.0);
    vec2 smallGhost = point + source * 0.35;
    if (dot(smallGhost, smallGhost) < 0.035 * 0.035) {
        float disc = 1.0 - smoothstep(0.018, 0.035, length(smallGhost));
        flare += vec3(1.0, 0.78, 0.18) * (0.18 * disc);
    }
    vec2 largeGhost = point + source * 0.85;
    // Beyond this radius the Gaussian tail is below one display level, even at maximum exposure/glare.
    if (dot(largeGhost, largeGhost) < 0.20 * 0.20) {
        float distance = length(largeGhost);
        flare += vec3(1.0, 0.72, 0.50) * (0.40 * exp2(-distance * distance * 1600.0));
        flare += vec3(1.0, 0.42, 0.16) * (0.10 * (1.0 - smoothstep(0.032, 0.052, distance)));
        // A soft, slightly colored crescent gives the larger reflection its photographic rim.
        vec2 axis = source / max(length(source), 0.001);
        float crescent = smoothstep(-0.4, 0.7, -dot(largeGhost, axis) / max(distance, 0.001));
        float ring = (distance - 0.115) / 0.018;
        vec3 rim = mix(vec3(0.85, 0.72, 0.22), vec3(1.0, 0.26, 0.06), smoothstep(0.10, 0.13, distance));
        flare += rim * (0.24 * exp2(-ring * ring) * crescent);
    }
    return flare * (u_sunGlare.z * visible);
}

void main() {
    float depth = depthAt(v_uv);
    vec4 scene = texture2D(u_scene, v_uv);
    vec4 atmosphere = vec4(0, 0, 0, 1);
    bool solidBase = false;
    if (u_fogEnabled > 0.5) solidBase = groundBaseSide(world(depth), depth, u_sand.z, 0.01 / u_sand.w);
    if (u_fogEnabled > 0.5 && !solidBase && v_uv.x >= u_scatteringBounds.x && v_uv.y >= u_scatteringBounds.y
          && v_uv.x <= u_scatteringBounds.z && v_uv.y <= u_scatteringBounds.w) {
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
        color = mix(u_horizon, u_sky, smoothstep(0.0, 1.0, v_uv.y));
    }
    vec3 linear = pow(max(color, vec3(0.0)), vec3(2.2));
    vec4 sand = sandLayer(depth);
    linear = (linear * sand.a + sand.rgb) * atmosphere.a + atmosphere.rgb;
    linear *= u_exposure * (1.0 + u_lightning);
    if (scene.a > 0.0) {
        // Sky colors already carry the time/cover palette; do not grade or desaturate them a second time.
        linear *= u_tint;
        float luminance = dot(linear, vec3(0.2126, 0.7152, 0.0722));
        linear = mix(vec3(luminance), linear, u_saturation);
    }
    // Lens glare borrows the active sun's color, after terrain grading and before display conversion/FoV.
    linear += sunGlare() * u_exposure;
    // The source artwork and scene target are LDR. An extra filmic curve would
    // amplify their baked contrast and destroy the tileset's original palette.
    linear = clamp(linear, 0.0, 1.0);
    color = pow(linear, vec3(1.0 / 2.2));
    vec2 edge = (v_uv - 0.5) * 2.0;
    color *= 1.0 - 0.09 * dot(edge, edge) * 0.5;
    if (u_fovEnabled > 0.5) color = fieldOfView(color, depth);
    gl_FragColor = vec4(color, 1.0);
    // Reuse the scene's hardware depth for weather and tactical occlusion in this same draw.
    DEPTH = depth;
}

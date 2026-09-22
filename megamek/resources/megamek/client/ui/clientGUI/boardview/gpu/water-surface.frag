// Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later
#ifdef GL_ES
precision mediump float;
#endif
varying vec2 v_diffuseUV;
varying vec3 v_normal;
varying vec4 v_color;
varying float v_opacity;
uniform sampler2D u_diffuseTexture;
#ifdef diffuseColorFlag
uniform vec4 u_diffuseColor;
#endif
uniform vec4 u_waterMaterial; // pool opacity, palette, rapids, procedural color
uniform vec3 u_waterPalette;
uniform vec3 u_waterBedColor;
uniform vec4 u_waterMotion; // world XY current / hex width, falling, receiving surface Z / hex width
uniform float u_waterEffects;
uniform int u_splashCount;
uniform vec4 u_splashEdges[6]; // center XY, inward XY with magnitude equal to the splash radius
const float WATER_PATTERN_SCALE = 0.085;
const float WATER_WAVE_STRENGTH = 0.10;
const float RAIN_CREST_CONTRAST = 0.20;

void main() {
    bool falling = u_waterMotion.z > 0.5;
    bool spray = !falling && v_normal.z < 0.5;
    vec2 position = v_cloudPosition.xy * u_rainScale;
    vec2 flow = position - u_waterMotion.xy * u_rainTime;
    if (falling) {
        // A fall keeps the surface's own field: the same world mapping, carried down by the height it has
        // fallen, so the pattern crosses the lip without a seam. Field units per second are unchanged.
        float fallen = u_waterMotion.w - v_cloudPosition.z * u_rainScale;
        flow = vec2(position.x, position.y - fallen + u_rainTime * 0.8);
    }
    // Two bounded, differently phased deformations evolve in place when there is no downstream current.
    vec2 phaseA = vec2(sin(u_rainTime * 0.71), cos(u_rainTime * 0.53)) * 0.006;
    vec2 phaseB = vec2(cos(u_rainTime * 0.47), sin(u_rainTime * 0.83)) * 0.008;
    vec3 fieldA = texture2D(u_rainNoise, flow * WATER_PATTERN_SCALE + phaseA).rgb;
    vec3 fieldB = texture2D(u_rainNoise, flow * WATER_PATTERN_SCALE + fieldA.gb * 0.008 + phaseB + 0.37).rgb;
    vec3 ripples = falling || spray ? vec3(0.0) : rainRippleField(position) * u_waterEffects;
    vec2 slope = (fieldA.gb + fieldB.rb - 1.0) * WATER_WAVE_STRENGTH * u_rainDetail * u_waterEffects + ripples.xy;
    float foam = 0.0, droplets = 0.0;
    if (u_splashCount > 0 && u_waterEffects * u_rainDetail > 0.0) {
        for (int i = 0; i < 6; i++) {
            if (i >= u_splashCount) break;
            vec2 delta = position - u_splashEdges[i].xy;
            float radius = length(u_splashEdges[i].zw);
            vec2 inward = u_splashEdges[i].zw / radius;
            float depth = dot(delta, inward);
            float across = dot(delta, vec2(-inward.y, inward.x));
            float footprint = (1.0 - smoothstep(0.18, 0.27, abs(across)))
                  * (1.0 - smoothstep(0.0, radius * 1.8, depth)) * step(-0.015, depth);
            if (footprint <= 0.0) continue;
            float turbulence = texture2D(u_rainNoise, vec2(across * 0.7, depth * 0.5 - u_rainTime * 0.12)).g;
            if (spray) {
                float cell = floor(across * 36.0);
                vec3 seed = rainHash(vec3(cell, float(i), 43.0));
                float cycle = u_rainTime * mix(1.4, 2.2, seed.x) + seed.y;
                float age = fract(cycle);
                vec3 impact = rainHash(vec3(cell, floor(cycle), float(i) + 91.0));
                float height = (v_cloudPosition.z * u_rainScale - u_waterMotion.w);
                float arc = 4.0 * age * (1.0 - age) * radius * mix(0.45, 1.0, impact.x);
                float horizontal = fract(across * 36.0) - mix(0.2, 0.8, impact.y);
                float drop = (1.0 - smoothstep(0.08, 0.25, abs(horizontal)))
                      * (1.0 - smoothstep(0.004, 0.018, abs(height - arc)));
                droplets = max(droplets, drop * footprint * (1.0 - smoothstep(0.7, 1.0, age)));
            } else {
                float wake = sin(depth * 70.0 - u_rainTime * 10.0 + turbulence * 3.0);
                slope += inward * wake * footprint * 0.24 * u_waterEffects;
                float boiling = (0.5 + 0.5 * turbulence) * (1.0 - smoothstep(radius * 0.3, radius * 1.5, depth));
                foam = max(foam, footprint * (boiling + max(0.0, wake) * 0.22));
            }
        }
    }
    vec3 normal = falling ? normalize(v_normal) : normalize(vec3(slope, 1.0));
    vec4 diffuse;
    if (u_waterMaterial.w > 0.5) {
        // The material supplies the depth/theme palette once; pixels only evaluate the changing pattern.
        float ridge = 1.0 - smoothstep(0.012, 0.075, abs(fieldA.r - fieldB.g));
        vec3 color = u_waterPalette * mix(0.86, 1.10, fieldB.b);
        color = mix(color, vec3(0.86, 0.91, 0.87), ridge * 0.39);
        float rapids = u_waterMaterial.z * 0.18;
        if (falling) rapids += 0.15;
        color = mix(color, vec3(0.86, 0.91, 0.87), smoothstep(0.55, 0.82, fieldA.b) * rapids);
        diffuse = vec4(color, 1.0);
    } else {
        diffuse = texture2D(u_diffuseTexture, v_diffuseUV);
    }
    vec3 tint = vec3(1.0);
#ifdef diffuseColorFlag
    tint = u_diffuseColor.rgb;
#endif
    vec3 albedo = diffuse.rgb * tint * v_color.rgb;
    if (falling) {
        // Pools inherit their warm bed through transparency. Carry that same color response over the lip;
        // its average contributes only the bed palette, never a painted rock/sand pattern on the fall.
        albedo = mix(u_waterBedColor, albedo, u_waterMaterial.x);
    }
    // Small signed crest contrast supplements refraction at the board camera's steep viewing angle.
    albedo *= 1.0 + clamp(ripples.z, -1.0, 1.0) * RAIN_CREST_CONTRAST;
    foam = clamp(foam * u_waterEffects * u_rainDetail, 0.0, 0.95);
    vec3 foamColor = vec3(0.82, 0.91, 0.88) * mix(vec3(1.0), tint, 0.6);
    albedo = spray ? foamColor : mix(albedo, foamColor, foam);
#ifdef lightingFlag
    vec3 ambient, direct, sheen;
    float film = falling || spray ? 0.0 : u_rainDetail * u_waterEffects * (1.0 - foam);
    surfaceLighting(normal, film, ambient, direct, sheen);
    albedo *= ambient + direct;
    albedo += sheen;
    if (film > 0.0) albedo = rainReflection(albedo, normal, film * 0.8);
#endif
    float alpha = mix(diffuse.a * v_opacity, 0.88, foam);
    if (spray) alpha = droplets * 0.7 * u_rainDetail * u_waterEffects;
    if (alpha <= 0.001) discard;
    gl_FragColor = vec4(albedo, alpha);
}

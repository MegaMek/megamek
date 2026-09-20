// Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later
// Ground only. DefaultShader owns the uniforms, material binding and directional shadow map.
#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_diffuseUV;
varying vec3 v_normal;
varying vec4 v_color;
uniform sampler2D u_diffuseTexture;
uniform sampler2D u_normalTexture;
uniform float u_normalMaps;

#ifdef lightingFlag
uniform vec3 u_ambientCubemap[6];
#if numDirectionalLights > 0
struct DirectionalLight {
    vec3 color;
    vec3 direction;
};
uniform DirectionalLight u_dirLights[numDirectionalLights];
#endif

#ifdef shadowMapFlag
uniform sampler2D u_shadowTexture;
uniform float u_shadowPCFOffset;
varying vec3 v_shadowMapUv;

float shadowSample(vec2 offset) {
    vec4 depthChannels = texture2D(u_shadowTexture, v_shadowMapUv.xy + offset);
    float depth = dot(depthChannels, vec4(1.0, 1.0 / 255.0, 1.0 / 65025.0, 1.0 / 16581375.0));
    return step(v_shadowMapUv.z, depth);
}
#endif
#endif

void main() {
    vec3 albedo = texture2D(u_diffuseTexture, v_diffuseUV).rgb * v_color.rgb;
    vec3 normal = normalize(v_normal);
    // Ground UVs project world X/right and -Y/down, including slopes and riverbanks.
    // The map's neutral texel is exactly (128,128,255), so paved surfaces retain their face normal.
    if (u_normalMaps > 0.5) {
        vec3 detail = (texture2D(u_normalTexture, v_diffuseUV).rgb * 255.0 - 128.0) / 127.0;
        vec3 tangent = normalize(vec3(normal.z, 0.0, -normal.x));
        normal = normalize(tangent * detail.x - cross(normal, tangent) * detail.y + normal * detail.z);
    }

#ifdef lightingFlag
    vec3 squared = normal * normal;
    vec3 positive = step(vec3(0.0), normal);
    vec3 ambient = squared.x * mix(u_ambientCubemap[0], u_ambientCubemap[1], positive.x)
          + squared.y * mix(u_ambientCubemap[2], u_ambientCubemap[3], positive.y)
          + squared.z * mix(u_ambientCubemap[4], u_ambientCubemap[5], positive.z);
    vec3 direct = vec3(0.0);
#if numDirectionalLights > 0
    for (int i = 0; i < numDirectionalLights; i++) {
        direct += u_dirLights[i].color * max(0.0, dot(normal, -u_dirLights[i].direction));
    }
#endif
#ifdef shadowMapFlag
    float offset = u_shadowPCFOffset;
    direct *= 0.25 * (shadowSample(vec2(offset, offset)) + shadowSample(vec2(-offset, offset))
          + shadowSample(vec2(offset, -offset)) + shadowSample(vec2(-offset, -offset)));
#endif
    albedo *= ambient + direct;
#endif
    gl_FragColor = vec4(albedo, 1.0);
}

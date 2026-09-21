// Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later
// Ground and water share per-fragment lighting and geometry-shadow sampling.
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

void surfaceLighting(vec3 normal, float film, out vec3 ambient, out vec3 direct, out vec3 sheen) {
    vec3 squared = normal * normal;
    vec3 positive = step(vec3(0.0), normal);
    ambient = squared.x * mix(u_ambientCubemap[0], u_ambientCubemap[1], positive.x)
          + squared.y * mix(u_ambientCubemap[2], u_ambientCubemap[3], positive.y)
          + squared.z * mix(u_ambientCubemap[4], u_ambientCubemap[5], positive.z);
    direct = vec3(0.0);
    sheen = vec3(0.0);
#if numDirectionalLights > 0
    vec3 view = -u_viewDirection;
    for (int i = 0; i < numDirectionalLights; i++) {
        vec3 light = -u_dirLights[i].direction;
        float incidence = max(0.0, dot(normal, light));
        direct += u_dirLights[i].color * incidence;
        if (film > 0.0 && incidence > 0.0) {
            vec3 halfVector = normalize(light + view);
            float exponent = mix(12.0, 96.0, film);
            float fresnel = 0.02 + 0.98 * pow(1.0 - max(0.0, dot(view, halfVector)), 5.0);
            float specular = pow(max(0.0, dot(normal, halfVector)), exponent) * (exponent + 2.0) / 8.0;
            sheen += u_dirLights[i].color * incidence * film * fresnel * specular;
        }
    }
#endif
#ifdef shadowMapFlag
    float offset = u_shadowPCFOffset;
    float visibility = 0.25 * (shadowSample(vec2(offset, offset)) + shadowSample(vec2(-offset, offset))
          + shadowSample(vec2(offset, -offset)) + shadowSample(vec2(-offset, -offset)));
    direct *= visibility;
    sheen *= visibility;
#endif
}
#endif

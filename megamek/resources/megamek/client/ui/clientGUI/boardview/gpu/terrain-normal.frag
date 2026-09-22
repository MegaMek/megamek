// Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later
// Ground only. DefaultShader owns the uniforms, material binding and directional shadow map.
#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_diffuseUV;
varying vec3 v_normal;
varying vec4 v_color;
uniform sampler2D u_diffuseTexture;
#ifdef normalTextureFlag
uniform sampler2D u_normalTexture;
uniform float u_normalMaps;
#endif
uniform float u_groundResponse;

void main() {
    vec3 albedo = texture2D(u_diffuseTexture, v_diffuseUV).rgb * v_color.rgb;
    vec3 normal = normalize(v_normal);
    // Ground UVs project world X/right and -Y/down, including slopes and riverbanks.
    // The map's neutral texel is exactly (128,128,255), so paved surfaces retain their face normal.
#ifdef normalTextureFlag
    if (u_normalMaps > 0.5) {
        vec3 detail = (texture2D(u_normalTexture, v_diffuseUV).rgb * 255.0 - 128.0) / 127.0;
        vec3 tangent = normalize(vec3(normal.z, 0.0, -normal.x));
        normal = normalize(tangent * detail.x - cross(normal, tangent) * detail.y + normal * detail.z);
    }
#endif
    // Only exposed, upward-facing ground receives liquid water. No accumulation or terrain-rule changes.
    float wet = u_wetness * step(0.0, u_groundResponse) * smoothstep(0.2, 0.8, v_normal.z);
    float response = max(0.0, u_groundResponse);
    albedo *= 1.0 - wet * mix(0.175, 0.10, response); // This is the darkening of the terrain (the mix(min, max, ...))
    float puddle = 0.0;
    if (wet * u_rainDetail > 0.0) {
        vec2 position = v_cloudPosition.xy * u_rainScale;
        puddle = rainPuddle(position, wet, response)
              * smoothstep(0.97, 0.999, v_normal.z) * u_rainDetail;
        if (puddle > 0.0) {
            albedo *= 1.0 - 0.18 * puddle;
            vec3 waterNormal = normalize(vec3(rainRipples(position), 1.0));
            normal = normalize(mix(normal, waterNormal, puddle));
        }
    }

#ifdef lightingFlag
    vec3 ambient, direct, sheen;
    surfaceLighting(normal, wet * mix(response, 1.0, puddle), ambient, direct, sheen);
    albedo *= ambient + direct;
    albedo += sheen;
    if (puddle > 0.0) { albedo = rainReflection(albedo, normal, puddle); }
#endif
    gl_FragColor = vec4(albedo, 1.0);
}

// Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later
// Inserted before libGDX's main; GpuUnitShader supplies the calls from its vertex shader.
uniform vec2 u_camoRotation;
uniform float u_camoEnabled;
uniform vec2 u_camoImageSize;
uniform mat4 u_paintTransform;
uniform mat3 u_paintNormalMatrix;
uniform vec4 u_damageTransform;
varying vec3 v_paintPosition;
varying vec2 v_damageUV;
varying float v_damageMask;

void unitMaterialCoordinates() {
    v_damageMask = 1.0;
    #ifdef colorFlag
        // The exporter's PALETTE['glass'] identifies cockpit glazing inside shared detail meshes.
        // Test the original vertex color, before player paint, damage or lighting can change it.
        vec3 glassDifference = a_color.rgb - vec3(0.21, 0.67, 0.73);
        v_damageMask = step(0.000025, dot(glassDifference, glassDifference));
    #endif
    v_paintPosition = (u_paintTransform * vec4(a_position, 1.0)).xyz;
    // Rest-space projection stays fixed to each rigid part, including unpainted metal.
    v_damageUV = a_position.xy * 0.04;
    #ifdef normalFlag
        vec3 damageNormal = abs(a_normal);
        if (damageNormal.x > damageNormal.y && damageNormal.x > damageNormal.z) {
            v_damageUV = a_position.yz * 0.04;
        } else if (damageNormal.y > damageNormal.z) {
            v_damageUV = a_position.xz * 0.04;
        }
    #endif
    v_damageUV = vec2(u_damageTransform.x * v_damageUV.x - u_damageTransform.y * v_damageUV.y,
                      u_damageTransform.y * v_damageUV.x + u_damageTransform.x * v_damageUV.y)
          + u_damageTransform.zw;
}

#ifdef diffuseTextureFlag
vec2 unitDiffuseUV() {
    // Sprite fallbacks and other authored textures retain their own UVs.
    if (u_camoEnabled < 0.5) {
        return u_diffuseUVTransform.xy + a_texCoord0 * u_diffuseUVTransform.zw;
    }
    // One image spans the complete rest-pose unit on each projection plane, including attachments.
    // Position and normal transforms are captured before posing, so animation cannot move the paint.
    vec2 uv = v_paintPosition.xy;
    #ifdef normalFlag
        vec3 n = abs(u_paintNormalMatrix * a_normal);
        if (n.x > n.y && n.x > n.z) {
            uv = vec2(1.0 - v_paintPosition.y, v_paintPosition.z);
        } else if (n.y > n.z) {
            uv = v_paintPosition.xz;
        }
    #endif
    // EntityImage rotates around the image center in pixels, preserving the image's aspect ratio.
    vec2 centeredUV = (uv - vec2(0.5)) * u_camoImageSize;
    vec2 rotatedUV = vec2(u_camoRotation.x * centeredUV.x - u_camoRotation.y * centeredUV.y,
                         u_camoRotation.y * centeredUV.x + u_camoRotation.x * centeredUV.y);
    return u_diffuseUVTransform.xy + (rotatedUV / u_camoImageSize + vec2(0.5)) * u_diffuseUVTransform.zw;
}
#endif

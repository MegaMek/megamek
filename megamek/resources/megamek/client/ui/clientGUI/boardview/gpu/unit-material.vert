// Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later
// Inserted before libGDX's main; GpuUnitShader supplies the calls from its vertex shader.
uniform vec2 u_camoRotation;
uniform vec3 u_camoRestScale;
uniform mat4 u_markerTransform;
uniform vec4 u_damageTransform;
varying vec2 v_markerUV;
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
    v_markerUV = (u_markerTransform * vec4(a_position, 1.0)).xy;
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
    vec2 restScale = vec2(1.0);
    #ifdef normalFlag
        // Match the exporter's two least-normal axes, including its X/Y/Z tie order.
        vec3 n = abs(a_normal);
        if (n.x <= n.y && n.x <= n.z) {
            restScale = vec2(u_camoRestScale.x, n.y <= n.z ? u_camoRestScale.y : u_camoRestScale.z);
        } else if (n.y <= n.z) {
            restScale = vec2(u_camoRestScale.y, n.x <= n.z ? u_camoRestScale.x : u_camoRestScale.z);
        } else {
            restScale = vec2(u_camoRestScale.z, n.x <= n.y ? u_camoRestScale.x : u_camoRestScale.y);
        }
    #endif
    vec2 centeredUV = a_texCoord0 * restScale - vec2(0.5);
    vec2 rotatedUV = vec2(u_camoRotation.x * centeredUV.x - u_camoRotation.y * centeredUV.y,
                         u_camoRotation.y * centeredUV.x + u_camoRotation.x * centeredUV.y);
    return u_diffuseUVTransform.xy + (rotatedUV + vec2(0.5)) * u_diffuseUVTransform.zw;
}
#endif

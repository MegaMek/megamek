// Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later
#ifndef cloudVolumeFlag
varying vec3 v_cloudPosition;
#endif
#ifdef cloudShadowFlag
uniform sampler2D u_cloudShadow;
uniform mat4 u_cloudProjection;

float cloudTransmission(vec3 position) {
    vec3 uv = (u_cloudProjection * vec4(position, 1.0)).xyz;
    if (uv.x < 0.0 || uv.y < 0.0 || uv.x > 1.0 || uv.y > 1.0 || uv.z >= 1.0) return 1.0;
    // The atlas integrates the layer from its base. Surfaces within/above it receive only the remaining column.
    return pow(max(texture2D(u_cloudShadow, uv.xy).r, 0.003), clamp(1.0 - uv.z, 0.0, 1.0));
}
#endif

// Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later
attribute vec3 a_position;
attribute vec2 a_texCoord0;
varying vec2 v_uv;

#ifdef VERTEX_SUN_VISIBILITY
uniform sampler2D u_depth;
uniform vec4 u_sunGlare;
varying float v_sunVisibility;
// SUN_VISIBILITY
#endif

void main() {
    v_uv = a_texCoord0;
    gl_Position = vec4(a_position, 1.0);
    #ifdef VERTEX_SUN_VISIBILITY
        v_sunVisibility = u_sunGlare.z > 0.0 ? sunVisibility() : 0.0;
    #endif
}

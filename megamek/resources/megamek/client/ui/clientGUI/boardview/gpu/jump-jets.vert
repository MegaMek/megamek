// Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later
attribute vec3 a_position;
attribute vec2 a_texCoord0;
attribute vec2 a_texCoord1;
uniform mat4 u_projView;
varying vec2 v_uv;
varying vec2 v_effect;
void main() {
    v_uv = a_texCoord0;
    v_effect = a_texCoord1;
    gl_Position = u_projView * vec4(a_position, 1.0);
}

// Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later
attribute vec3 a_position;
attribute vec2 a_texCoord0;
varying vec2 v_uv;

void main() {
    v_uv = a_texCoord0;
    gl_Position = vec4(a_position, 1.0);
}

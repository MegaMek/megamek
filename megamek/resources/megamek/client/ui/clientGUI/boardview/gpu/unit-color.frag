// Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later
#ifdef GL_ES
precision mediump float;
#endif
uniform vec4 u_outlineColor;

void main() {
    gl_FragColor = vec4(u_outlineColor.rgb, 1.0);
}

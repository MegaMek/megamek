// Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later
#ifdef GL_ES
#extension GL_EXT_frag_depth : require
precision highp float;
#define DEPTH gl_FragDepthEXT
#else
#define DEPTH gl_FragDepth
#endif
varying vec2 v_uv;
uniform sampler2D u_depth;

void main() {
    DEPTH = min(1.0, dot(texture2D(u_depth, v_uv),
        vec4(1.0, 1.0 / 255.0, 1.0 / 65025.0, 1.0 / 16581375.0)));
    gl_FragColor = vec4(0.0);
}

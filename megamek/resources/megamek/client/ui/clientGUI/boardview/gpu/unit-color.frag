// Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later
#ifdef GL_ES
precision mediump float;
#endif
#if defined(diffuseTextureFlag) && defined(blendedFlag)
varying vec2 v_texCoords0;
uniform sampler2D u_diffuseTexture;
uniform float u_alphaTest;
#endif
uniform vec4 u_outlineColor;

void main() {
    // Match libGDX DepthShader's cutout test; transparent sprite pixels must not write either attachment.
    #if defined(diffuseTextureFlag) && defined(blendedFlag)
    if (texture2D(u_diffuseTexture, v_texCoords0).a < u_alphaTest) discard;
    #endif
    gl_FragColor = vec4(u_outlineColor.rgb, 1.0);
}

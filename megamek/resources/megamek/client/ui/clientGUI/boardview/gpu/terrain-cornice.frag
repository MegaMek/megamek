// Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later
// Skirt strips only. Their art is a mask, tinted by the top layer they hang from and blended with its alpha.
// V stays inside the strip (see GpuAssets.cornice), so a mask's opposite edges are never blended together.
// DefaultShader owns the uniforms, material binding and directional shadow map.
#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_diffuseUV;
varying vec3 v_normal;
varying vec4 v_color;
uniform sampler2D u_diffuseTexture;
uniform float u_groundResponse;
// Rivulets across one strip width, their travel in strip heights per second, and what they darken.
const float RUNOFF_COLUMNS = 16.0;
const float RUNOFF_SPEED = 1.2;
const float RUNOFF_DARKENING = 0.22;

void main() {
    vec4 mask = texture2D(u_diffuseTexture, v_diffuseUV);
    // Alpha is the strip's shape, including the fade at its lower end. Gray is lightness about mid gray, so
    // the tint arrives unchanged at 128, darkens into the shadowed rows and lightens on the lit ones.
    vec3 albedo = v_color.rgb * mask.rgb * 2.0;
    float alpha = mask.a * v_color.a;
    vec3 normal = normalize(v_normal);
    // A skirt is a vertical face, so it takes the rain film the ground shares with it, using its own
    // material's response. It skips the ground's face-up gate and puddles: nothing pools on a wall.
    float response = max(0.0, u_groundResponse);
    float wet = u_wetness * step(0.0, u_groundResponse);
    float runOff = 0.0;
    if (wet > 0.0) {
        // Rain drains down the wall as rivulets. The strip's own UV keeps them vertical whichever way the
        // edge faces, and the clock is the one the ground's ripples already use.
        vec2 flow = vec2(v_diffuseUV.x * RUNOFF_COLUMNS, v_diffuseUV.y - u_rainTime * RUNOFF_SPEED);
        float broad = texture2D(u_rainNoise, flow).r;
        float fine = texture2D(u_rainNoise, flow * vec2(2.1, 3.3) + 0.37).r;
        runOff = smoothstep(0.52, 0.86, broad * 0.62 + fine * 0.38) * wet;
    }
    albedo *= 1.0 - wet * mix(0.175, 0.10, response) - runOff * RUNOFF_DARKENING;
#ifdef lightingFlag
    vec3 ambient, direct, sheen;
    // The ground scales its film by the material's response, so a skirt does too: a wall wets as much as the
    // material it faces. A rivulet is standing water, so it takes the whole film where it runs (mix to one).
    surfaceLighting(normal, wet * mix(response, 1.0, runOff), ambient, direct, sheen);
    albedo *= ambient + direct;
    albedo += sheen;
#endif
    gl_FragColor = vec4(albedo, alpha);
}

// Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later
attribute vec3 a_position;
attribute vec2 a_texCoord0;
uniform mat4 u_projView;
uniform vec3 u_origin;
uniform vec3 u_extent;
uniform vec3 u_right;
uniform vec3 u_up;
uniform vec2 u_wind;
uniform float u_clock;
uniform float u_level;
uniform float u_kind;
varying vec2 v_uv;
varying float v_kind;
varying float v_fade;

void main() {
    bool rain = u_kind < 0.5;
    bool snow = u_kind > 0.5 && u_kind < 1.5;
    bool sand = u_kind > 2.5;
    float speed = (rain ? 16.0 : snow ? 2.4 : sand ? 0.3 : 10.0) * u_level;
    vec3 seed = fract(a_position + vec3(0.173, 0.371, 0.619) * u_kind);
    vec3 center;
    // World-anchored motion within a wrapping, camera-bounded volume.
    center.xy = u_origin.xy + mod(seed.xy * u_extent.xy - u_origin.xy + u_wind * u_level * u_clock, u_extent.xy);
    float height = fract(seed.z - u_clock * speed / u_extent.z);
    center.z = u_origin.z + height * u_extent.z;
    if (snow || sand) {
        center.xy += vec2(sin(u_clock * 1.3 + seed.z * 30.0), cos(u_clock + seed.x * 30.0))
            * u_level * (snow ? 0.18 : 0.3);
    }
    if (sand) {
        center.z = u_origin.z + (center.z - u_origin.z) * 0.22;
    }
    vec3 velocity = vec3(u_wind * u_level, -speed);
    vec2 projected = vec2(dot(velocity, u_right), dot(velocity, u_up));
    float projectedLength = length(projected);
    vec2 axis = projectedLength > 0.001 ? projected / projectedLength : vec2(0.0, -1.0);
    vec3 along = u_right * axis.x + u_up * axis.y;
    vec3 across = u_right * -axis.y + u_up * axis.x;
    float width = (rain ? 0.025 : snow ? 0.055 : sand ? 0.04 : 0.065) * u_level;
    float trail = rain ? max(0.12 * u_level, projectedLength * 0.028) : sand ? width * 2.5 : width;
    vec3 position = center + across * a_texCoord0.x * width + along * a_texCoord0.y * trail;
    v_uv = a_texCoord0;
    v_kind = u_kind;
    v_fade = smoothstep(0.0, 0.12, height) * (1.0 - smoothstep(0.85, 1.0, height));
    gl_Position = u_projView * vec4(position, 1.0);
}

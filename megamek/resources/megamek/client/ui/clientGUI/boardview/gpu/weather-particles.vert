// Copyright (C) 2026 The MegaMek Team.
// SPDX-License-Identifier: GPL-3.0-or-later

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
uniform float u_pixelSize;
uniform float u_kind;
uniform float u_batch;

varying vec2 v_uv;
varying float v_kind;
varying float v_fade;

void main() {
    bool rain = u_kind < 0.5;
    bool snow = u_kind > 0.5 && u_kind < 1.5;
    bool sand = u_kind > 2.5;

    float speed = (rain ? 16.0 : snow ? 2.4 : 10.0) * u_level;

    vec3 seed = fract(
        a_position + vec3(0.173, 0.371, 0.619) * u_kind
            + vec3(0.457, 0.239, 0.853) * u_batch
    );

    vec3 center;

    // World-anchored motion within a wrapping, camera-bounded volume.
    center.xy =
        u_origin.xy +
        mod(
            seed.xy * u_extent.xy
                - u_origin.xy
                + u_wind * u_level * u_clock,
            u_extent.xy
        );

    float height =
        fract(seed.z - u_clock * speed / u_extent.z);

    center.z =
        u_origin.z + height * u_extent.z;

    vec3 velocity =
        vec3(u_wind * u_level, -speed);

    float fade =
        smoothstep(0.0, 0.12, height) *
        (1.0 - smoothstep(0.85, 1.0, height));

    if (snow) {
        center.xy +=
            vec2(
                sin(u_clock * 1.3 + seed.z * 30.0),
                cos(u_clock + seed.x * 30.0)
            )
            * u_level
            * 0.18;
    }

    if (sand) {
        vec2 wind = normalize(u_wind);
        vec2 crosswind = vec2(-wind.y, wind.x);

        // Nearby crosswind lanes share a gust.
        // Integrate its speed so grains never jump or reverse.
        float lane =
            dot(seed.xy * u_extent.xy, crosswind)
            / (u_level * 2.5);

        float gust =
            u_clock * 2.2 + lane;

        float variation =
            mix(
                0.72,
                1.28,
                fract(seed.x * 37.1 + seed.y * 11.7)
            );

        float travel =
            u_clock + 0.14 * sin(gust);

        float flutter =
            u_clock * 9.0 + seed.z * 40.0;

        vec2 drift =
            u_wind
                * u_level
                * variation
                * travel
            + crosswind
                * sin(flutter)
                * u_level
                * 0.06;

        center.xy =
            u_origin.xy +
            mod(
                seed.xy * u_extent.xy
                    - u_origin.xy
                    + drift,
                u_extent.xy
            );

        // Most grains skim the ground,
        // with a sparse lifted layer and rapid shallow hops.
        float hop =
            u_clock * (7.0 + seed.x * 5.0)
            + seed.y * 30.0;

        center.z =
            u_origin.z
            + u_level * 0.10
            + seed.z
                * seed.z
                * u_extent.z
                * 0.32
            + (0.5 + 0.5 * sin(hop))
                * u_level
                * 0.16;

        velocity =
            vec3(
                u_wind
                    * u_level
                    * variation
                    * (1.0 + 0.308 * cos(gust))
                + crosswind
                    * cos(flutter)
                    * u_level
                    * 0.54,

                cos(hop)
                    * (7.0 + seed.x * 5.0)
                    * u_level
                    * 0.06
            );

        vec2 edge =
            (center.xy - u_origin.xy)
            / u_extent.xy;

        vec2 edgeFade =
            smoothstep(
                vec2(0.0),
                vec2(0.04),
                edge
            )
            *
            (
                1.0 -
                smoothstep(
                    vec2(0.96),
                    vec2(1.0),
                    edge
                )
            );

        // Higher minimum opacity makes the sand
        // considerably easier to see during gusts.
        fade =
            edgeFade.x
            * edgeFade.y
            * mix(
                0.80,
                1.0,
                0.5 + 0.5 * sin(gust)
            );
    }

    vec2 projected =
        vec2(
            dot(velocity, u_right),
            dot(velocity, u_up)
        );

    float projectedLength =
        length(projected);

    vec2 axis =
        projectedLength > 0.001
            ? projected / projectedLength
            : vec2(0.0, -1.0);

    vec3 along =
        u_right * axis.x
        + u_up * axis.y;

    vec3 across =
        u_right * -axis.y
        + u_up * axis.x;

    float width =
        (rain ? 0.025 : snow ? 0.055 : 0.065)
        * u_level;

    float trail =
        rain
            ? max(
                0.12 * u_level,
                projectedLength * 0.028
            )
            : width;

    if (sand) {
        // Small grains, with a filtered footprint when zoomed out.
        float grain =
            mix(
                0.0225,
                0.036,
                fract(
                    seed.y * 19.3
                    + seed.z * 7.1
                )
            )
            * u_level;

        // Keep sand readable even when zoomed out.
        width =
            max(
                grain,
                u_pixelSize * 1.35
            );

        // Slightly longer wind-aligned trails
        // make movement easier to perceive.
        trail =
            max(
                width * 1.5,
                min(
                    u_level * 0.108,
                    projectedLength * 0.00225
                )
            );

        // Prevent the screen-space size clamp from
        // making particles excessively transparent.
        fade *=
            max(
                0.85,
                grain / width
            );
    }

    vec3 position =
        center
        + across * a_texCoord0.x * width
        + along * a_texCoord0.y * trail;

    v_uv = a_texCoord0;
    v_kind = u_kind;
    v_fade = fade;

    gl_Position =
        u_projView * vec4(position, 1.0);
}

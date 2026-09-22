/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.planetaryConditions.AtmosphericTaint;
import megamek.common.planetaryConditions.PlanetaryConditions;

/** Scenario-derived visual settings owned by the GPU thread; tuning never changes planetary game conditions. */
final class BoardAtmosphere {
    static final float MIN_GROUND_LAYER_HEIGHT = 1.0f;
    static final float STANDARD_GROUND_LAYER_HEIGHT = 2.0f;
    static final Settings DEFAULTS = new Settings(13, 0, 0, STANDARD_GROUND_LAYER_HEIGHT, 0, 0);
    static final float MAX_FOG_OPACITY = 0.25f;
    /** Fraction of ambient fill moved into visible sunlight, bounded by the LDR directional-light budget. */
    static final float DAYLIGHT_SHADOW_CONTRAST = 0.25f;
    /** Fraction of ambient fill moved into visible moonlight; 0 restores the original full-moon shadows. */
    static final float MOONLIGHT_SHADOW_CONTRAST = 0.7f;
    /** Artistic palette blends, not gas opacity or a change to gameplay visibility. */
    /** Palette weight per severity unit: the toxic blend is twice its tainted counterpart, and the default
     * multiplier of {@link #DEFAULT_TAINT_STRENGTH} stays below the 1.0 blend ceiling so severities and
     * pressures remain distinguishable. */
    static final float TAINTED_COLOR_STRENGTH = 0.1f;
    static final float TOXIC_COLOR_STRENGTH = 0.2f;
    static final float DEFAULT_TAINT_STRENGTH = 2.0f;
    /** Share of the palette blend that reaches the display-space grade applied to every drawn surface. */
    static final float TAINT_GRADE_SHARE = 0.25f;
    private static final int CAUSTIC_TAINT_COLOR = 0xc4c07aff;
    private static final int POISON_TAINT_COLOR = 0x9e8aa6ff;
    private static final int FLAMMABLE_TAINT_COLOR = 0xc58e68ff;
    /** Keep low-angle shadows about five caster-heights long without unbounded incidence compensation. */
    private static final float MIN_LIGHT_ALTITUDE = 0.24f;

    /** Ground layer height is in terrain levels; exposure is in stops. Night defaults to full moon unless explicitly suppressed. */
    record Settings(float hour, float clouds, float fog, float groundLayerHeight, float haze, float exposure,
          Effects effects, Atmosphere pressure, int temperature, boolean moonlight, AtmosphericTaint taint, float gravity) {
        Settings(float hour, float clouds, float fog, float groundLayerHeight, float haze, float exposure) {
            this(hour, clouds, fog, groundLayerHeight, haze, exposure, Effects.NONE);
        }

        Settings(float hour, float clouds, float fog, float groundLayerHeight, float haze, float exposure, Effects effects) {
            this(hour, clouds, fog, groundLayerHeight, haze, exposure, effects, Atmosphere.STANDARD);
        }

        Settings(float hour, float clouds, float fog, float groundLayerHeight, float haze, float exposure,
              Effects effects, Atmosphere pressure) {
            this(hour, clouds, fog, groundLayerHeight, haze, exposure, effects, pressure, 25);
        }

        Settings(float hour, float clouds, float fog, float groundLayerHeight, float haze, float exposure,
              Effects effects, Atmosphere pressure, int temperature) {
            this(hour, clouds, fog, groundLayerHeight, haze, exposure, effects, pressure, temperature, true);
        }

        Settings(float hour, float clouds, float fog, float groundLayerHeight, float haze, float exposure,
              Effects effects, Atmosphere pressure, int temperature, boolean moonlight) {
            this(hour, clouds, fog, groundLayerHeight, haze, exposure, effects, pressure, temperature, moonlight,
                  AtmosphericTaint.BREATHABLE);
        }

        Settings(float hour, float clouds, float fog, float groundLayerHeight, float haze, float exposure,
              Effects effects, Atmosphere pressure, int temperature, boolean moonlight, AtmosphericTaint taint) {
            this(hour, clouds, fog, groundLayerHeight, haze, exposure, effects, pressure, temperature, moonlight, taint, 1);
        }

        Settings {
            if (!Float.isFinite(hour) || !Float.isFinite(clouds) || !Float.isFinite(fog)
                  || !Float.isFinite(groundLayerHeight) || !Float.isFinite(haze) || !Float.isFinite(exposure)
                  || !Float.isFinite(gravity)) {
                throw new IllegalArgumentException("Atmosphere settings must be finite");
            }
            hour = ((hour % 24) + 24) % 24;
            clouds = MathUtils.clamp(clouds, 0, 1);
            fog = MathUtils.clamp(fog, 0, 1);
            groundLayerHeight = MathUtils.clamp(groundLayerHeight, MIN_GROUND_LAYER_HEIGHT, 8);
            haze = MathUtils.clamp(haze, 0, 1);
            exposure = MathUtils.clamp(exposure, -2, 2);
            gravity = Math.max(0, gravity);
            java.util.Objects.requireNonNull(effects);
            java.util.Objects.requireNonNull(pressure);
            java.util.Objects.requireNonNull(taint);
            if (pressure.isLighterThan(Atmosphere.THIN)) {
                clouds = 0;
            }
            if (pressure.isLighterThan(Atmosphere.STANDARD)) {
                fog = 0;
                effects = new Effects(0, 0, 0, effects.sand(), 0, effects.wind(), effects.windDirection());
            }
            if (pressure.isVacuum()) {
                haze = 0;
                effects = Effects.NONE;
            }
        }
    }

    /** Intensities are visual only. Wind direction is clockwise from north, in the direction particles travel. */
    record Effects(float rain, float snow, float hail, float sand, float lightning, float wind, float windDirection) {
        static final Effects NONE = new Effects(0, 0, 0, 0, 0, 0, 0);

        Effects {
            if (!Float.isFinite(rain) || !Float.isFinite(snow) || !Float.isFinite(hail) || !Float.isFinite(sand)
                  || !Float.isFinite(lightning) || !Float.isFinite(wind) || !Float.isFinite(windDirection)) {
                throw new IllegalArgumentException("Weather effects must be finite");
            }
            rain = MathUtils.clamp(rain, 0, 1);
            snow = MathUtils.clamp(snow, 0, 1);
            hail = MathUtils.clamp(hail, 0, 1);
            sand = MathUtils.clamp(sand, 0, 1);
            lightning = MathUtils.clamp(lightning, 0, 1);
            wind = MathUtils.clamp(wind, 0, 1);
            windDirection = ((windDirection % 360) + 360) % 360;
        }

        boolean hasParticles() {
            return rain > 0 || snow > 0 || hail > 0;
        }
    }

    /**
     * Derived afresh when settings change. Colors and direction are read-only to consumers. {@code sky},
     * {@code horizon} and {@code fog} color the air itself; {@code tint} and {@code saturation} grade every drawn
     * surface in the composite, which is the only stage that sees the board and the sky together.
     */
    record Lighting(Vector3 direction, Color direct, Color ambient, Color fog, Color sky, Color horizon, Color tint,
          float saturation, float daylight, boolean sunlight) {
        /** Exposure scale, in photographic stops, at full daylight and at full night. */
        private static final float MIDDAY_STOPS = 0.8f;
        private static final float NIGHT_STOPS = -0.4f;

        boolean hasDirectLight() {
            return direct.r > 0 || direct.g > 0 || direct.b > 0;
        }

        /** Keep the light at the same screen bearing through the board camera's yaw, tilt, pan and zoom. */
        Lighting relativeTo(Camera camera) {
            // The source stays above the screen and board, including the overhead camera pole.
            Vector3 relative = new Vector3(camera.direction).crs(camera.up).nor().scl(direction.x)
                  .mulAdd(camera.up, -Math.abs(direction.y)).mulAdd(camera.direction, -direction.z).nor();
            // Tilting changes incidence on level ground; retain its established direct illumination.
            float compensation = direction.z / relative.z;
            Color compensated = new Color(direct).mul(compensation, compensation, compensation, 1);
            return new Lighting(relative, compensated, ambient, fog, sky, horizon, tint, saturation, daylight, sunlight);
        }

        float exposureScale(float compensation) {
            float night = 1 - daylight;
            return (float) Math.pow(2, compensation + (MIDDAY_STOPS * daylight) + (NIGHT_STOPS * night));
        }
    }

    /** A visual cloud layer, in hex widths. This does not invent meteorological game conditions. */
    record Clouds(float altitude, float thickness, float density, float stratus, float scattering) { }

    static Clouds clouds(Settings settings) {
        Effects effects = settings.effects();
        float storm = Math.max(effects.rain(), Math.max(effects.hail(), effects.lightning()));
        float stratus = Math.max(settings.fog(), Math.max(effects.snow(),
              Math.max(0, (settings.clouds() - 0.7f) / 0.3f) * 0.65f));
        float pressure = switch (settings.pressure()) {
            case VACUUM, TRACE -> 0;
            case THIN -> 0.35f;
            case STANDARD -> 1;
            case HIGH -> 1.2f;
            case VERY_HIGH -> 1.4f;
        };
        boolean thin = settings.pressure().isThin();
        return new Clouds(thin ? 9 : 5 - storm * 1.5f, thin ? 0.6f : (1.5f + storm * 2.5f) * pressure,
              (4.0f + storm * 3) * pressure, thin ? 1 : stratus,
              pressure * (0.003f + settings.haze() * 0.045f + settings.fog() * 0.02f));
    }

    /** Liquid rain wets the ground immediately; this renderer does not simulate accumulation or drying. */
    static float wetness(Settings settings) {
        return permitsWetness(settings) ? settings.effects().rain() : 0;
    }

    static boolean permitsWetness(Settings settings) {
        return settings.pressure().isDenserThan(Atmosphere.THIN) && settings.temperature() > 0;
    }

    private BoardAtmosphere() { }

    /** Follow changed scenario fields unless their visual control was overridden; game conditions remain untouched. */
    static Settings followScenario(Settings current, Settings previous, Settings next) {
        Effects a = current.effects(), b = previous.effects(), c = next.effects();
        return new Settings(follow(current.hour(), previous.hour(), next.hour()),
              follow(current.clouds(), previous.clouds(), next.clouds()),
              follow(current.fog(), previous.fog(), next.fog()),
              follow(current.groundLayerHeight(), previous.groundLayerHeight(), next.groundLayerHeight()),
              follow(current.haze(), previous.haze(), next.haze()),
              follow(current.exposure(), previous.exposure(), next.exposure()),
              new Effects(follow(a.rain(), b.rain(), c.rain()), follow(a.snow(), b.snow(), c.snow()),
                    follow(a.hail(), b.hail(), c.hail()), follow(a.sand(), b.sand(), c.sand()),
                    follow(a.lightning(), b.lightning(), c.lightning()),
                    follow(a.wind(), b.wind(), c.wind()),
                    follow(a.windDirection(), b.windDirection(), c.windDirection())),
              current.pressure() == previous.pressure() ? next.pressure() : current.pressure(),
              current.temperature() == previous.temperature() ? next.temperature() : current.temperature(),
              current.moonlight() == previous.moonlight() ? next.moonlight() : current.moonlight(),
              current.taint() == previous.taint() ? next.taint() : current.taint(),
              follow(current.gravity(), previous.gravity(), next.gravity()));
    }

    private static float follow(float current, float previous, float next) {
        return MathUtils.isEqual(current, previous, 0.00001f) ? next : current;
    }

    /** Map a caller-owned conditions copy; publish immutable settings instead of sharing game conditions with GL. */
    static Settings fromScenario(PlanetaryConditions conditions, boolean inSpace, double timeSample) {
        if (!Double.isFinite(timeSample) || timeSample < 0 || timeSample >= 1) {
            throw new IllegalArgumentException("Scenario time sample must be in [0, 1)");
        }
        // One visual sample per GPU window, reused by snapshots and previews. Never consume game-rule randomness.
        float hour = switch (conditions.getLight()) {
            case DAY, GLARE, SOLAR_FLARE -> hourInWindow(8, 17, timeSample);
            // Twilight occurs at both ends of the day; sun altitude is mirrored across sunrise and sunset.
            case DUSK_DAWN -> timeSample < 0.5 ? hourInWindow(6f, 6.75f, timeSample * 2)
                  : hourInWindow(17.25f, 18f, (timeSample - 0.5) * 2);
            case FULL_MOON, MOONLESS, PITCH_BLACK -> hourInWindow(20, 28, timeSample);
        };
        float exposure = switch (conditions.getLight()) {
            case DAY, DUSK_DAWN, FULL_MOON -> 0; //these are driven by exposureScale only
            case GLARE -> 0.6f;
            case SOLAR_FLARE -> 1.2f;
            case MOONLESS -> -0.6f;
            case PITCH_BLACK -> -1.0f;
        };
        boolean air = !inSpace && !conditions.getAtmosphere().isVacuum();
        // The scenario editor permits precipitation and fog only in standard or denser atmospheres.
        boolean weather = air && conditions.getAtmosphere().isDenserThan(Atmosphere.THIN);
        float rain = 0, snow = 0, hail = 0, lightning = 0;
        if (weather) {
            switch (conditions.getWeather()) {
                case CLEAR -> { }
                case LIGHT_RAIN -> rain = 0.25f;
                case MOD_RAIN -> rain = 0.5f;
                case HEAVY_RAIN -> rain = 0.7f;
                case GUSTING_RAIN -> rain = 0.85f;
                case DOWNPOUR -> rain = 1;
                case LIGHT_SNOW -> snow = 0.25f;
                case MOD_SNOW -> snow = 0.5f;
                case SNOW_FLURRIES -> snow = 0.7f;
                case HEAVY_SNOW -> snow = 1;
                case SLEET -> { rain = 0.35f; snow = 0.35f; }
                case ICE_STORM -> { rain = 0.5f; hail = 0.5f; }
                case LIGHT_HAIL -> hail = 0.25f;
                case HEAVY_HAIL -> hail = 1;
                case LIGHTNING_STORM -> { rain = 0.5f; lightning = 0.65f; }
            }
        }
        float wind = !air || conditions.getWindDirection().isRandomWindDirection() ? 0 : switch (conditions.getWind()) {
            case CALM -> 0;
            case LIGHT_GALE -> 0.2f;
            case MOD_GALE -> 0.4f;
            case STRONG_GALE -> 0.6f;
            case STORM -> 0.8f;
            case TORNADO_F1_TO_F3, TORNADO_F4 -> 1;
        };
        float direction = switch (conditions.getWindDirection()) {
            case SOUTH, RANDOM -> 0;
            case SOUTHWEST -> 60;
            case NORTHWEST -> 120;
            case NORTH -> 180;
            case NORTHEAST -> 240;
            case SOUTHEAST -> 300;
        };
        float sand = air && conditions.isBlowingSandActive() ? 0.6f : 0;
        // Ground fog also implies a hazier sky, so it lifts cloud cover without replacing precipitation clouds.
        float fogDensity = 0;
        float fogClouds = 0;
        if (weather) {
            switch (conditions.getFog()) {
                case FOG_NONE -> { }
                case FOG_LIGHT -> { fogDensity = 0.2f; fogClouds = 0.10f; }
                case FOG_HEAVY -> { fogDensity = 1; fogClouds = 0.25f; }
            }
        }
        float precipitation = Math.max(rain, Math.max(snow, hail));
        float clouds = Math.max(fogClouds, precipitation > 0 ? MathUtils.lerp(0f, 0.8f, precipitation) : 0);
        // Low fog pools around the terrain; haze scales its presence above that layer.
        float pressureReduction = switch (conditions.getAtmosphere()) {
            case HIGH -> 0.75f;
            case VERY_HIGH -> 1.5f;
            default -> 0;
        };
        float height = fogDensity > 0 ? Math.max(MIN_GROUND_LAYER_HEIGHT, STANDARD_GROUND_LAYER_HEIGHT - pressureReduction)
              : DEFAULTS.groundLayerHeight();
        // Sand supplies its own bounded veil in the composite; it does not need the fog/haze pass.
        float haze = fogDensity;
        return new Settings(hour, clouds, fogDensity, height, haze, exposure,
              new Effects(rain, snow, hail, sand, lightning, wind, direction),
              inSpace ? Atmosphere.VACUUM : conditions.getAtmosphere(), conditions.getTemperature(),
              !conditions.getLight().isMoonlessOrPitchBack(), conditions.getAtmosphericTaint(), conditions.getGravity());
    }

    /** Inclusive quarter-hour choices match the tuning slider; an end above 24 crosses midnight. */
    private static float hourInWindow(float start, float end, double sample) {
        int choices = Math.round((end - start) * 4) + 1;
        return (start + (int) (sample * choices) * 0.25f) % 24;
    }

    private static float cloudiness(Settings settings) {
        return settings.clouds() * (settings.pressure().isThin() ? 0.35f : 1);
    }

    static Lighting lighting(Settings settings) {
        return lighting(settings, MOONLIGHT_SHADOW_CONTRAST);
    }

    static Lighting lighting(Settings settings, float moonShadowContrast) {
        return lighting(settings, moonShadowContrast, DEFAULT_TAINT_STRENGTH);
    }

    static Lighting lighting(Settings settings, float moonShadowContrast, float taintStrength) {
        // Coverage affects the sky palette. Spatial transmission alone attenuates surface sunlight.
        float clouds = cloudiness(settings);
        float angle = (settings.hour() - 6) * MathUtils.PI / 12;
        float altitude = MathUtils.sin(angle);
        float daylight = smooth(-0.14f, 0.25f, altitude);
        float scattering = switch (settings.pressure()) {
            case VACUUM -> 0;
            case TRACE -> 0.15f;
            case THIN -> 0.45f;
            default -> 1;
        };
        // Warm light peaks at the horizon, independently of how much daylight remains.
        float warmth = smooth(-0.20f, 0.015f, altitude) * (1 - smooth(0.16f, 0.50f, altitude)) * scattering;
        float twilight = (1 - smooth(0.05f, 0.45f, Math.abs(altitude))) * scattering;
        Color warm = settings.hour() < 12 ? new Color(1, 0.53f, 0.33f, 1) : new Color(1, 0.43f, 0.20f, 1);
        // The visible sunrise/sunset windows use the sun. Fade both sources to zero at their handover,
        // so a moon color never illuminates from the sun's direction (or flips a visible shadow).
        float sunVisibility = smooth(-0.08f, 0, altitude);
        boolean sunlight = altitude > -0.08f;
        float sun = sunVisibility * (0.55f + 0.45f * Math.max(0, altitude));
        float moonVisibility = settings.moonlight() ? 1 - smooth(-0.18f, -0.08f, altitude) : 0;
        float moon = moonVisibility * 0.14f;
        float side = sunlight ? 1 : -1;
        // A small north/south component avoids the shadow camera's pole, even at solar noon.
        Vector3 direction = new Vector3(-MathUtils.cos(angle) * side, -0.35f * side,
              -Math.max(MIN_LIGHT_ALTITUDE, Math.abs(altitude)) * 0.9f).nor();
        Color direct = new Color(1, 0.96f, 0.86f, 1).lerp(warm, warmth);
        direct.mul(sun * 0.3f);
        direct.add(0.45f * moon, 0.62f * moon, moon, 0);
        Color ambient = new Color(0.62f, 0.69f, 0.82f, 1)
              .lerp(new Color(0.53f, 0.56f, 0.60f, 1), daylight);
        // Horizon light is mostly diffuse. Keep that fill while the sun and moon exchange directions.
        ambient.add(0.16f * twilight, 0.14f * twilight, 0.1f * twilight, 0);
        // Give diffuse horizon light a warm cast without removing the established readability fill.
        float ambientLuminance = luminance(ambient);
        Color warmFill = new Color(1, 0.74f, 0.64f, 1);
        float energy = ambientLuminance / (0.2126f + 0.7152f * warmFill.g + 0.0722f * warmFill.b);
        warmFill.mul(energy, energy, energy, 1);
        ambient.lerp(warmFill, warmth * 0.65f);
        // Preserve ambient + direct * -direction.z on lit ground while strengthening sun/moon shadows.
        // Both transfers fade with their source; bound compensation by every RGB channel at low angles.
        float compensation = (DAYLIGHT_SHADOW_CONTRAST * sunVisibility + moonShadowContrast * moonVisibility)
              / -direction.z;
        compensation = Math.min(compensation, Math.min((0.95f - direct.r) / ambient.r,
              Math.min((0.95f - direct.g) / ambient.g, (0.95f - direct.b) / ambient.b)));
        float transfer = compensation * -direction.z;
        direct.add(ambient.r * compensation, ambient.g * compensation, ambient.b * compensation, 0);
        ambient.mul(1 - transfer, 1 - transfer, 1 - transfer, 1);
        Color fog = new Color(0.3f, 0.39f, 0.55f, 1)
              .lerp(new Color(0.57f, 0.68f, 0.79f, 1), daylight)
              .lerp(warm, warmth * (1 - clouds * 0.65f) * 0.65f)
              .lerp(new Color(0.64f, 0.49f, 0.3f, 1), settings.effects().sand());
        Color sky = new Color(0.035f, 0.055f, 0.12f, 1)
              .lerp(new Color(0.12f, 0.26f, 0.45f, 1), daylight)
              .lerp(new Color(0.22f, 0.13f, 0.31f, 1), twilight * 0.65f);
        Color horizon = new Color(0.085f, 0.10f, 0.17f, 1)
              .lerp(new Color(0.42f, 0.56f, 0.69f, 1), daylight)
              .lerp(new Color(0.40f, 0.23f, 0.41f, 1), twilight)
              .lerp(new Color(warm).mul(0.75f, 0.75f, 0.75f, 1), warmth * 0.9f);
        // A smooth cloud-colored veil is strongest overhead and fades toward the horizon. No sky texture/pass.
        Color overcast = new Color(0.065f, 0.075f, 0.11f, 1)
              .lerp(new Color(0.48f, 0.52f, 0.57f, 1), daylight)
              .lerp(new Color(0.48f, 0.32f, 0.34f, 1), twilight * 0.55f);
        sky.lerp(overcast, clouds * 0.92f);
        horizon.lerp(overcast, clouds * 0.10f);
        if (settings.pressure().isVacuum()) {
            sky.set(0.002f, 0.003f, 0.006f, 1);
            horizon.set(sky);
            fog.set(sky);
        }
        Color tint = new Color(0.82f, 0.92f, 1.1f, 1).lerp(Color.WHITE, daylight)
              .lerp(new Color(1.12f, 0.93f, 0.82f, 1), warmth * 0.85f);
        if (!settings.taint().isBreathable() && scattering > 0 && taintStrength > 0) {
            // Hazard categories do not specify gas composition: these restrained hues are visual cues only.
            Color palette = new Color(switch (settings.taint()) {
                case TAINTED_CAUSTIC, TOXIC_CAUSTIC -> CAUSTIC_TAINT_COLOR;
                case TAINTED_POISON, TOXIC_POISON -> POISON_TAINT_COLOR;
                case TAINTED_FLAME, TOXIC_FLAME -> FLAMMABLE_TAINT_COLOR;
                case BREATHABLE -> throw new IllegalStateException("Breathable air has no taint palette");
            });
            // Warm horizon light owns dawn and dusk, so the palette recedes while it shines.
            float strength = (settings.taint().isToxic() ? TOXIC_COLOR_STRENGTH : TAINTED_COLOR_STRENGTH)
                  * MathUtils.clamp(taintStrength, 0, 10) * scattering
                  * MathUtils.lerp(0.2f, 1, daylight) * (1 - warmth * 0.75f);
            // Existing gradients/volumes provide depth. Weather alone decides whether scattering is rendered.
            tintAtmosphere(sky, palette, strength * 0.35f);
            tintAtmosphere(horizon, palette, strength);
            tintAtmosphere(fog, palette, strength);
            // The composite grades every drawn surface, so the air reaches the board and not only the sky.
            tintAtmosphere(tint, palette, strength * TAINT_GRADE_SHARE);
        }
        return new Lighting(direction, direct, ambient, fog, sky, horizon, tint,
              0.78f + 0.22f * daylight, daylight, sunlight);
    }

    private static float luminance(Color color) {
        return 0.2126f * color.r + 0.7152f * color.g + 0.0722f * color.b;
    }

    /** Blend a palette into a color at that color's own luminance: tinting adds no emission or exposure. */
    private static void tintAtmosphere(Color color, Color palette, float strength) {
        // Above 1 the lerp overshoots the luminance-matched target and channel clamping would then break it.
        float blend = MathUtils.clamp(strength, 0, 1);
        if (blend <= 0) { return; }
        float energy = luminance(color) / luminance(palette);
        color.lerp(new Color(palette).mul(energy, energy, energy, 1), blend);
    }

    private static float smooth(float low, float high, float value) {
        float t = MathUtils.clamp((value - low) / (high - low), 0, 1);
        return t * t * (3 - 2 * t);
    }
}

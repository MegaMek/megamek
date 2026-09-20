/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.planetaryConditions.PlanetaryConditions;

/** Scenario-derived visual settings owned by the GPU thread; tuning never changes planetary game conditions. */
final class BoardAtmosphere {
    static final float MIN_FOG_HEIGHT = 1.0f;
    static final float STANDARD_FOG_HEIGHT = 2.5f;
    static final Settings DEFAULTS = new Settings(13, 0, 0, STANDARD_FOG_HEIGHT, 0, 0);
    static final float MAX_FOG_OPACITY = 0.4f;

    /** Fog height is in terrain levels; exposure is in photographic stops. */
    record Settings(float hour, float clouds, float fog, float fogHeight, float haze, float exposure,
          Effects effects) {
        Settings(float hour, float clouds, float fog, float fogHeight, float haze, float exposure) {
            this(hour, clouds, fog, fogHeight, haze, exposure, Effects.NONE);
        }

        Settings {
            if (!Float.isFinite(hour) || !Float.isFinite(clouds) || !Float.isFinite(fog)
                  || !Float.isFinite(fogHeight) || !Float.isFinite(haze) || !Float.isFinite(exposure)) {
                throw new IllegalArgumentException("Atmosphere settings must be finite");
            }
            hour = ((hour % 24) + 24) % 24;
            clouds = MathUtils.clamp(clouds, 0, 1);
            fog = MathUtils.clamp(fog, 0, 1);
            fogHeight = MathUtils.clamp(fogHeight, MIN_FOG_HEIGHT, 8);
            haze = MathUtils.clamp(haze, 0, 1);
            exposure = MathUtils.clamp(exposure, -2, 2);
            java.util.Objects.requireNonNull(effects);
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
            return rain > 0 || snow > 0 || hail > 0 || sand > 0;
        }
    }

    enum Weather {
        CLEAR("Clear", 0, 0, STANDARD_FOG_HEIGHT, 0),
        OVERCAST("Overcast", 1, 0.04f, 3, 0.06f),
        MIST("Mist", 0.35f, 0.18f, 1.5f, 0.1f),
        FOG("Fog", 0.85f, 0.6f, 3, 0.15f);

        final String label;
        private final float clouds;
        private final float fog;
        private final float height;
        private final float haze;

        Weather(String label, float clouds, float fog, float height, float haze) {
            this.label = label;
            this.clouds = clouds;
            this.fog = fog;
            this.height = height;
            this.haze = haze;
        }

        Settings apply(Settings current) {
            return new Settings(current.hour(), clouds, fog, height, haze, current.exposure(),
                  new Effects(0, 0, 0, 0, 0, current.effects().wind(), current.effects().windDirection()));
        }
    }

    /** Derived afresh when settings change. Colors and direction are read-only to consumers. */
    record Lighting(Vector3 direction, Color direct, Color ambient, Color fog, Color sky, Color tint,
          float saturation, float daylight) {
        /** Exposure scale, in photographic stops, at full daylight and at full night. */
        private static final float MIDDAY_STOPS = 0.8f;
        private static final float NIGHT_STOPS = -0.8f;

        float exposureScale(float compensation) {
            float night = 1 - daylight; // night is ^2
            return (float) Math.pow(2, compensation + (MIDDAY_STOPS * daylight) + (NIGHT_STOPS * night * night));
        }
    }

    private BoardAtmosphere() { }

    /** Read only on Swing; publish the immutable visual result instead of sharing mutable game conditions with GL. */
    static Settings fromScenario(PlanetaryConditions conditions, boolean inSpace) {
        // Scenarios describe lighting categories, not a precise astronomical clock.
        float hour = switch (conditions.getLight()) {
            case DAY, GLARE, SOLAR_FLARE -> 13;
            case DUSK -> 18;
            case FULL_MOON, MOONLESS, PITCH_BLACK -> 0;
        };
        float exposure = switch (conditions.getLight()) {
            case DAY, DUSK, FULL_MOON -> 0;
            case GLARE -> 0.2f;
            case SOLAR_FLARE -> 0.4f;
            case MOONLESS -> -0.3f;
            case PITCH_BLACK -> -0.6f;
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
                case FOG_LIGHT -> { fogDensity = 0.2f; fogClouds = 0.15f; }
                case FOG_HEAVY -> { fogDensity = 1; fogClouds = 0.3f; }
            }
        }
        float precipitation = Math.max(rain, Math.max(snow, hail));
        float clouds = Math.max(fogClouds,
              precipitation >= 0.75f ? 1 : precipitation >= 0.5f ? 0.85f : precipitation > 0 ? 0.65f : 0);
        // Low fog pools around the terrain; haze scales its presence above that layer.
        float pressureReduction = switch (conditions.getAtmosphere()) {
            case HIGH -> 0.75f;
            case VERY_HIGH -> 1.5f;
            default -> 0;
        };
        float height = fogDensity > 0 ? Math.max(MIN_FOG_HEIGHT, STANDARD_FOG_HEIGHT - pressureReduction)
              : DEFAULTS.fogHeight();
        float haze = Math.max(fogDensity, sand * 0.25f);
        return new Settings(hour, clouds, fogDensity, height, haze, exposure,
              new Effects(rain, snow, hail, sand, lightning, wind, direction));
    }

    static Lighting lighting(Settings settings) {
        // The full slider range ends at the old 60% cloud cover, retaining directional shadows.
        float clouds = settings.clouds() * 0.6f;
        float angle = (settings.hour() - 6) * MathUtils.PI / 12;
        float altitude = MathUtils.sin(angle);
        float daylight = smooth(-0.12f, 0.22f, altitude);
        float warmth = (1 - smooth(0.05f, 0.55f, altitude)) * daylight;
        float sun = smooth(-0.03f, 0.18f, altitude) * (0.55f + 0.45f * Math.max(0, altitude));
        float transmission = 1 - clouds * 0.88f;
        float moon = (1 - daylight) * 0.14f * transmission;
        float side = altitude >= 0 ? 1 : -1;
        // A small north/south component avoids the shadow camera's pole, even at solar noon.
        Vector3 direction = new Vector3(-MathUtils.cos(angle) * side, -0.35f * side,
              -Math.max(0.12f, Math.abs(altitude)) * 0.9f).nor();
        Color direct = new Color(1, 0.96f, 0.86f, 1).lerp(new Color(1, 0.43f, 0.18f, 1), warmth);
        direct.mul(sun * transmission * 0.3f);
        direct.add(0.45f * moon, 0.62f * moon, moon, 0);
        Color ambient = new Color(0.62f, 0.69f, 0.82f, 1)
              .lerp(new Color(0.53f, 0.56f, 0.60f, 1), daylight);
        ambient.lerp(new Color(0.70f, 0.73f, 0.77f, 1), clouds * daylight);
        // Horizon light is mostly diffuse. Keep that fill while the sun and moon exchange directions.
        float twilight = 1 - smooth(0.05f, 0.45f, Math.abs(altitude));
        ambient.add(0.16f * twilight, 0.14f * twilight, 0.1f * twilight, 0);
        Color fog = new Color(0.3f, 0.39f, 0.55f, 1)
              .lerp(new Color(0.57f, 0.68f, 0.79f, 1), daylight)
              .lerp(new Color(0.9f, 0.47f, 0.23f, 1), warmth * (1 - clouds) * 0.6f)
              .lerp(new Color(0.64f, 0.49f, 0.3f, 1), settings.effects().sand());
        Color sky = new Color(0.07f, 0.11f, 0.19f, 1)
              .lerp(new Color(0.16f, 0.29f, 0.43f, 1), daylight)
              .lerp(new Color(0.23f, 0.27f, 0.32f, 1), clouds * daylight);
        Color tint = new Color(0.82f, 0.92f, 1.1f, 1).lerp(Color.WHITE, daylight)
              .lerp(new Color(1.12f, 0.9f, 0.75f, 1), warmth * 0.65f);
        return new Lighting(direction, direct, ambient, fog, sky, tint,
              0.78f + 0.22f * daylight - 0.12f * clouds, daylight);
    }

    private static float smooth(float low, float high, float value) {
        float t = MathUtils.clamp((value - low) / (high - low), 0, 1);
        return t * t * (3 - 2 * t);
    }
}

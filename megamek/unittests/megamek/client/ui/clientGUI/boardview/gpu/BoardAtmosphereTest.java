/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.HashSet;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.GdxNativesLoader;
import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.planetaryConditions.BlowingSand;
import megamek.common.planetaryConditions.Fog;
import megamek.common.planetaryConditions.Light;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.planetaryConditions.Weather;
import megamek.common.planetaryConditions.Wind;
import megamek.common.planetaryConditions.WindDirection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BoardAtmosphereTest {
    @Test
    void sunCrossesTheBoardAndNightKeepsCoolReadableLighting() {
        var morning = BoardAtmosphere.lighting(at(9));
        var evening = BoardAtmosphere.lighting(at(15));
        var noon = BoardAtmosphere.lighting(at(12));
        var night = BoardAtmosphere.lighting(at(0));
        assertTrue(morning.direction().x < 0 && evening.direction().x > 0);
        assertTrue(noon.direction().z < morning.direction().z, "Noon casts shorter shadows");
        assertTrue(night.ambient().b > night.ambient().r, "Night has a cool ambient fill");
        assertTrue(night.ambient().r > 0 && night.direct().b > 0, "Night retains ambient and moonlight");
        assertTrue(noon.direct().r > night.direct().r, "Daylight has a stronger warm directional component");
        assertEquals(BoardAtmosphere.lighting(at(0)), BoardAtmosphere.lighting(at(24)));
    }

    @Test
    void cameraRelativeLightKeepsItsScreenDirectionAndLevelGroundBrightness() {
        GdxNativesLoader.load();
        for (float hour : new float[] { 0, 4, 6, 8, 12, 17, 18, 20 }) {
            var world = BoardAtmosphere.lighting(at(hour));
            Vector3 originalDirection = world.direction().cpy();
            Vector3 screenDirection = null;
            for (float tilt : new float[] { 0, 55, BoardCamera.MAX_TILT }) {
                for (int bearing = 0; bearing < 360; bearing += 60) {
                    var camera = new BoardCamera();
                    camera.camera.viewportWidth = 1280;
                    camera.camera.viewportHeight = 800;
                    camera.orbit(bearing, tilt);
                    var fixed = world.relativeTo(camera.camera);
                    Vector3 screen = fixed.direction().cpy().rot(camera.camera.view);
                    if (screenDirection == null) { screenDirection = screen; }
                    assertTrue(screenDirection.epsilonEquals(screen, 0.00001f),
                          "The light must stay fixed on screen through rotation, tilt and the overhead pole");
                    assertTrue(fixed.direction().z < -0.05f, "The source must remain above the terrain");
                    assertEquals(1, fixed.direction().len(), 0.00001f);
                    for (int channel = 0; channel < 3; channel++) {
                        float before = new float[] { world.direct().r, world.direct().g, world.direct().b }[channel];
                        float after = new float[] { fixed.direct().r, fixed.direct().g, fixed.direct().b }[channel];
                        assertEquals(before * -world.direction().z, after * -fixed.direction().z, 0.00001f);
                        assertTrue(Float.isFinite(after) && after >= 0 && after < 1);
                    }
                    assertEquals(world.ambient(), fixed.ambient());
                    assertEquals(world.exposureScale(0), fixed.exposureScale(0));
                    camera.pan(120, -90);
                    camera.zoom(0.8f);
                    assertEquals(fixed, world.relativeTo(camera.camera), "Pan and zoom must not move the light");
                }
            }
            assertEquals(originalDirection, world.direction(), "Camera presentation must not mutate the world lighting");
        }
    }

    @Test
    void onlyFullMoonNightsHaveDirectionalMoonlightAndManualClockChangesPreserveThatChoice() {
        var conditions = new PlanetaryConditions();
        assertTrue(BoardAtmosphere.lighting(at(0)).hasDirectLight(), "Unspecified night previews default to full moon");
        for (Light category : new Light[] { Light.FULL_MOON, Light.MOONLESS, Light.PITCH_BLACK }) {
            conditions.setLight(category);
            var settings = BoardAtmosphere.fromScenario(conditions, false, 0.5);
            boolean moon = category == Light.FULL_MOON;
            assertEquals(moon, settings.moonlight());
            assertEquals(moon, BoardAtmosphere.lighting(settings).hasDirectLight());
            var manualDay = new BoardAtmosphere.Settings(13, 0, 0, 2.5f, 0, settings.exposure(),
                  settings.effects(), settings.pressure(), settings.temperature(), settings.moonlight());
            assertTrue(BoardAtmosphere.lighting(manualDay).hasDirectLight(), "Suppressing moonlight must not suppress the sun");
            var day = BoardAtmosphere.DEFAULTS;
            var followed = BoardAtmosphere.followScenario(day, day, settings);
            assertEquals(moon, BoardAtmosphere.lighting(followed).hasDirectLight());
            assertEquals(category, conditions.getLight());
        }
    }

    @Test
    void allClockPositionsHaveFiniteDownwardLightingWithoutAShadowCameraPole() {
        for (float clouds : new float[] { 0, 0.5f, 1 }) {
            for (int quarter = 0; quarter <= 96; quarter++) {
                var light = BoardAtmosphere.lighting(new BoardAtmosphere.Settings(quarter / 4f, clouds, 0, 2, 0, 0));
                assertEquals(1, light.direction().len(), 0.0001);
                assertTrue(light.direction().z < -0.05f);
                assertTrue(Math.abs(light.direction().y) > 0.05f);
                for (float channel : new float[] { light.direct().r, light.direct().g, light.direct().b,
                      light.ambient().r, light.ambient().g, light.ambient().b }) {
                    assertTrue(Float.isFinite(channel) && channel >= 0 && channel < 1,
                          "Low-angle compensation must not clip lighting at " + quarter / 4f + ", clouds " + clouds);
                }
            }
        }
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
          0, 0, 0.67871630, 0.77089798, 0.95048064
          9, 0, 0.69356787, 0.71702516, 0.74066842
          12, 0, 0.80960137, 0.82841736, 0.84045720
          13, 0, 0.79414392, 0.81357819, 0.82716382
          """)
    void twilightPalettePreservesEstablishedDayAndNightGroundColors(float hour, float clouds, float red, float green, float blue) {
        // Captured before the light-balance change; only twilight's palette is intentionally retuned.
        var light = BoardAtmosphere.lighting(new BoardAtmosphere.Settings(hour, clouds, 0, 2, 0, 0));
        float incidence = -light.direction().z;
        assertEquals(red, light.ambient().r + incidence * light.direct().r, 0.00001f);
        assertEquals(green, light.ambient().g + incidence * light.direct().g, 0.00001f);
        assertEquals(blue, light.ambient().b + incidence * light.direct().b, 0.00001f);
    }

    @Test
    void everyScenarioTwilightChoiceHasAWarmHorizonAndWarmReadableGround() {
        var conditions = new PlanetaryConditions();
        conditions.setLight(Light.DUSK_DAWN);
        for (int sample = 0; sample < 256; sample++) {
            var settings = BoardAtmosphere.fromScenario(conditions, false, sample / 256.0);
            var light = BoardAtmosphere.lighting(settings);
            assertTrue(light.sunlight() && light.hasDirectLight(), "Every dawn/dusk choice uses a visible sun");
            assertTrue(light.horizon().r > light.horizon().b * 1.8f, "Dawn and dusk both need a warm horizon");
            assertTrue(light.sky().b > light.sky().r, "The upper sky retains its cool twilight gradient");
            float red = light.ambient().r - light.direction().z * light.direct().r;
            float blue = light.ambient().b - light.direction().z * light.direct().b;
            assertTrue(red > blue * 1.1f, "The board must share the horizon's warm light at " + settings.hour());
        }
    }

    @Test
    void skyColorsChangeContinuouslyThroughDayTwilightAndNight() {
        var previous = BoardAtmosphere.lighting(at(0));
        // Resolve the short source handover at ten-second intervals, as well as the broader sky palette.
        for (int step = 1; step <= 24 * 360; step++) {
            float hour = step / 360f;
            var next = BoardAtmosphere.lighting(at(hour));
            assertTrue(colorDistance(previous.sky(), next.sky()) < 0.01f, "Sky discontinuity at " + hour);
            assertTrue(colorDistance(previous.horizon(), next.horizon()) < 0.01f, "Horizon discontinuity at " + hour);
            assertTrue(colorDistance(previous.ambient(), next.ambient()) < 0.01f, "Ambient discontinuity at " + hour);
            previous = next;
        }
    }

    @Test
    void cloudCoverTintsTheSkyFromTheTopAtEveryTimeOfDay() {
        for (float hour : new float[] { 0, 5, 6, 6.75f, 12, 17.25f, 18, 19 }) {
            var clear = BoardAtmosphere.lighting(at(hour));
            float lastChange = 0;
            for (float cover : new float[] { 0.25f, 0.6f, 1 }) {
                var cloudy = BoardAtmosphere.lighting(new BoardAtmosphere.Settings(hour, cover, 0, 2.5f, 0, 0));
                float overheadChange = colorDistance(clear.sky(), cloudy.sky());
                assertTrue(overheadChange > lastChange, "Every increase in cover must affect the sky");
                assertTrue(overheadChange > colorDistance(clear.horizon(), cloudy.horizon()),
                      "The cloud veil fades toward the horizon at " + hour);
                lastChange = overheadChange;
            }
        }
        var vacuum = BoardAtmosphere.lighting(new BoardAtmosphere.Settings(6, 1, 0, 2.5f, 0, 0,
              BoardAtmosphere.Effects.NONE, Atmosphere.VACUUM));
        assertEquals(vacuum.sky(), vacuum.horizon(), "Airless worlds have no atmospheric twilight gradient");
        assertTrue(vacuum.sky().r < 0.01f && vacuum.sky().g < 0.01f && vacuum.sky().b < 0.01f);
    }

    private static float colorDistance(Color first, Color second) {
        return Math.abs(first.r - second.r) + Math.abs(first.g - second.g) + Math.abs(first.b - second.b);
    }

    @Test
    void sourceDirectionColorAndVisibilityAgreeThroughoutTheSunMoonHandover() {
        var previous = BoardAtmosphere.lighting(at(0));
        int handovers = 0;
        for (int minute = 1; minute <= 24 * 60; minute++) {
            var settings = at(minute / 60f);
            var light = BoardAtmosphere.lighting(settings);
            var moonless = BoardAtmosphere.lighting(new BoardAtmosphere.Settings(settings.hour(), settings.clouds(),
                  settings.fog(), settings.groundLayerHeight(), settings.haze(), settings.exposure(), settings.effects(),
                  settings.pressure(), settings.temperature(), false));
            if (light.sunlight()) {
                assertEquals(light, moonless, "A moon setting must not tint sunlight or change its shadows");
            } else {
                assertFalse(moonless.hasDirectLight(), "Afterglow must not invent sunlight or moonlight");
                assertTrue(light.direct().b >= light.direct().r, "A directional moon must use its cool light color");
            }
            if (light.sunlight() != previous.sunlight()) {
                handovers++;
                assertTrue(light.direct().r + light.direct().g + light.direct().b < 0.06f);
                assertTrue(previous.direct().r + previous.direct().g + previous.direct().b < 0.06f,
                      "The source must fade out before its shadow direction flips");
            }
            previous = light;
        }
        assertEquals(2, handovers);
    }

    @Test
    void daylightAndBothTwilightWindowsHaveDefinedShadowsWhileNightRetainsDiffuseFill() {
        float clear = shadowShare(BoardAtmosphere.lighting(at(13)));
        assertTrue(clear > 0.45f && clear < 0.60f, "Clear daylight retains sky fill without washing out cast shadows");
        for (float hour : new float[] { 6, 6.25f, 6.5f, 6.75f, 17.25f, 17.5f, 17.75f, 18 }) {
            var light = BoardAtmosphere.lighting(at(hour));
            float shade = shadowShare(light);
            assertTrue(shade > 0.55f && shade < 0.85f,
                  "Warm twilight needs visible cast shadows and readable fill at " + hour + "; shadow/lit = " + shade);
            float length = (float) Math.hypot(light.direction().x, light.direction().y) / -light.direction().z;
            assertTrue(length > 4, "Twilight shadows must stretch beyond four times the caster height");
            assertTrue(hour < 12 ? light.direction().x < 0 : light.direction().x > 0,
                  "Dawn and dusk must cast shadows in opposite directions");
        }
        float moonShade = shadowShare(BoardAtmosphere.lighting(at(0)));
        assertTrue(moonShade > 0.7f && moonShade < 0.85f, "Full Moon shadows should be visible while retaining readable fill");
    }

    @Test
    void moonContrastPreservesLitGroundAndOnlyAffectsTheVisibleMoon() {
        for (float hour : new float[] { 0, 4, 5.5f, 6, 12, 18, 18.5f, 20, 23 }) {
            var settings = at(hour);
            var original = BoardAtmosphere.lighting(settings, 0);
            float previousShade = shadowShare(original);
            for (float contrast : new float[] { BoardAtmosphere.MOONLIGHT_SHADOW_CONTRAST, 0.5f, 1 }) {
                var light = BoardAtmosphere.lighting(settings, contrast);
                if (light.sunlight()) {
                    assertEquals(original, light, "Moon contrast must not change daylight or dawn/dusk");
                } else {
                    assertTrue(shadowShare(light) <= previousShade + 0.00001f, "Increasing contrast must strengthen shadows");
                    assertTrue(shadowShare(light) < shadowShare(original) - 0.02f);
                    previousShade = shadowShare(light);
                    assertTrue(light.direct().b > light.direct().r, "Moonlight retains its cool color");
                }
                float incidence = -light.direction().z;
                assertEquals(original.ambient().r + incidence * original.direct().r,
                      light.ambient().r + incidence * light.direct().r, 0.00001f);
                assertEquals(original.ambient().g + incidence * original.direct().g,
                      light.ambient().g + incidence * light.direct().g, 0.00001f);
                assertEquals(original.ambient().b + incidence * original.direct().b,
                      light.ambient().b + incidence * light.direct().b, 0.00001f);
                assertEquals(original.sky(), light.sky());
                assertEquals(original.horizon(), light.horizon());
                assertEquals(original.exposureScale(0), light.exposureScale(0));
                for (float channel : new float[] { light.direct().r, light.direct().g, light.direct().b,
                      light.ambient().r, light.ambient().g, light.ambient().b }) {
                    assertTrue(Float.isFinite(channel) && channel >= 0 && channel < 1);
                }
                var moonless = new BoardAtmosphere.Settings(hour, 0, 0, 2, 0, 0,
                      BoardAtmosphere.Effects.NONE, Atmosphere.STANDARD, 25, false);
                assertEquals(BoardAtmosphere.lighting(moonless, 0), BoardAtmosphere.lighting(moonless, contrast),
                      "Moonless and Pitch Black must retain their original lighting");
            }
        }
    }

    private static float shadowShare(BoardAtmosphere.Lighting light) {
        Color ambient = light.ambient(), direct = light.direct();
        float fill = 0.2126f * ambient.r + 0.7152f * ambient.g + 0.0722f * ambient.b;
        float sun = 0.2126f * direct.r + 0.7152f * direct.g + 0.0722f * direct.b;
        return fill / (fill - light.direction().z * sun);
    }

    @Test
    void twilightAndOvercastNightsKeepAReadableIlluminationFloor() {
        for (float clouds : new float[] { 0, 1 }) {
            for (int quarter = 0; quarter < 96; quarter++) {
                var light = BoardAtmosphere.lighting(new BoardAtmosphere.Settings(quarter / 4f,
                      clouds, 0, 2, 0, 0));
                float ambient = 0.2126f * light.ambient().r + 0.7152f * light.ambient().g + 0.0722f * light.ambient().b;
                float direct = 0.2126f * light.direct().r + 0.7152f * light.direct().g + 0.0722f * light.direct().b;
                assertTrue(ambient - light.direction().z * direct >= 0.53f,
                      "No dark gap in the sun/moon handover at " + quarter / 4f + ", cloud cover " + clouds);
            }
        }
    }

    @Test
    void weatherPresetsUseRealScenarioFogAndCloudCoverage() {
        var clear = AtmospherePreset.CLEAR.settings(0.5);
        assertEquals(0, clear.clouds(), "Clear must remove all cloud cover");
        assertEquals(0, clear.fog());
        assertEquals(0, clear.haze());
        var conditions = new PlanetaryConditions();
        conditions.setFog(Fog.FOG_LIGHT);
        assertEquals(BoardAtmosphere.fromScenario(conditions, false, 0.5), AtmospherePreset.LIGHT_FOG.settings(0.5));
        conditions.setFog(Fog.FOG_HEAVY);
        assertEquals(BoardAtmosphere.fromScenario(conditions, false, 0.5), AtmospherePreset.HEAVY_FOG.settings(0.5));
        var clearNoon = BoardAtmosphere.lighting(new BoardAtmosphere.Settings(12, 0, 0, 2.5f, 0, 0));
        assertTrue(clearNoon.exposureScale(0) > 1.5f, "Neutral daytime exposure must lift the dim LDR scene");
    }

    @Test
    void cloudOpeningsRetainClearSkyIncidentLightWithoutGlobalDoubleAttenuation() {
        for (int hour = 0; hour < 24; hour++) {
            var clear = BoardAtmosphere.lighting(new BoardAtmosphere.Settings(hour, 0, 0, 2.5f, 0, 0));
            for (float cover : new float[] { 0.25f, 0.6f, 1 }) {
                var cloudy = BoardAtmosphere.lighting(new BoardAtmosphere.Settings(hour, cover, 0, 2.5f, 0, 0));
                assertEquals(clear.direct(), cloudy.direct(), "The spatial cloud integral handles sun/moon attenuation");
                assertEquals(clear.ambient(), cloudy.ambient(), "Cloud shadows retain the established ambient floor");
                assertEquals(clear.tint(), cloudy.tint());
                assertEquals(clear.saturation(), cloudy.saturation());
                assertEquals(clear.exposureScale(0), cloudy.exposureScale(0));
            }
        }
    }

    @Test
    void liquidRainDrivesWetnessOnlyInAppropriatePlanetaryConditions() {
        PlanetaryConditions conditions = new PlanetaryConditions();
        conditions.setWeather(Weather.DOWNPOUR);
        var rainy = BoardAtmosphere.fromScenario(conditions, false, 0.5);
        assertEquals(1, BoardAtmosphere.wetness(rainy));
        conditions.setTemperature(-5);
        var frozen = BoardAtmosphere.fromScenario(conditions, false, 0.5);
        assertEquals(0, BoardAtmosphere.wetness(frozen));
        assertFalse(BoardAtmosphere.permitsWetness(frozen));
        assertEquals(-5, BoardAtmosphere.followScenario(rainy, rainy, frozen).temperature());
        conditions.setTemperature(25);
        for (Atmosphere pressure : new Atmosphere[] { Atmosphere.VACUUM, Atmosphere.TRACE, Atmosphere.THIN }) {
            conditions.setAtmosphere(pressure);
            assertEquals(0, BoardAtmosphere.wetness(BoardAtmosphere.fromScenario(conditions, false, 0.5)));
        }
        conditions.setAtmosphere(Atmosphere.STANDARD);
        assertEquals(0, BoardAtmosphere.wetness(BoardAtmosphere.fromScenario(conditions, true, 0.5)));
        conditions.setWeather(Weather.HEAVY_SNOW);
        assertEquals(0, BoardAtmosphere.wetness(BoardAtmosphere.fromScenario(conditions, false, 0.5)));
    }

    @Test
    void settingsRejectNonFiniteValuesAndBoundShaderInputs() {
        assertThrows(IllegalArgumentException.class,
              () -> new BoardAtmosphere.Settings(Float.NaN, 0, 0, 1, 0, 0));
        var bounded = new BoardAtmosphere.Settings(-1, 2, -1, 0, 3, 8);
        assertEquals(23, bounded.hour());
        assertEquals(1, bounded.clouds());
        assertEquals(0, bounded.fog());
        assertEquals(1.0f, bounded.groundLayerHeight());
        assertEquals(2, bounded.exposure());
        assertThrows(IllegalArgumentException.class,
              () -> new BoardAtmosphere.Effects(0, 0, 0, 0, 0, Float.POSITIVE_INFINITY, 0));
        var effects = new BoardAtmosphere.Effects(2, -1, 3, -4, 5, 6, -60);
        assertEquals(new BoardAtmosphere.Effects(1, 0, 1, 0, 1, 1, 300), effects);
    }

    @Test
    void scenarioLightCategoriesChooseDayTwilightOrReadableNight() {
        PlanetaryConditions conditions = new PlanetaryConditions();
        var initial = BoardAtmosphere.fromScenario(conditions, false, 0.5);
        assertEquals(initial, AtmospherePreset.CLEAR.settings(0.5));
        for (Light light : Light.values()) {
            conditions.setLight(light);
            var settings = BoardAtmosphere.fromScenario(conditions, false, 0.5);
            float daylight = BoardAtmosphere.lighting(settings).daylight();
            if (light == Light.DUSK_DAWN) {
                assertTrue(daylight > 0 && daylight < 1, "Dusk must use the twilight transition");
            } else if (light.isDuskOrFullMoonOrMoonlessOrPitchBack()) {
                assertEquals(0, daylight);
                float expectedExposure = switch (light) {
                    case MOONLESS -> -0.6f;
                    case PITCH_BLACK -> -1.0f;
                    default -> 0;
                };
                assertEquals(expectedExposure, settings.exposure(), "Scenario exposure for " + light);
            } else {
                assertEquals(1, daylight);
            }
            assertEquals(light, conditions.getLight(), "Visual initialization must not alter game conditions");
        }
    }

    @Test
    void sampledTimesSpanTheConditionWindowOnTheTuningGridAndRetainItsLighting() {
        var conditions = new PlanetaryConditions();
        for (Light category : Light.values()) {
            conditions.setLight(category);
            var hours = new HashSet<Float>();
            for (int index = 0; index < 256; index++) {
                double sample = index / 256.0;
                var settings = BoardAtmosphere.fromScenario(conditions, false, sample);
                float hour = settings.hour();
                hours.add(hour);
                assertEquals(Math.round(hour * 4), hour * 4, "Chosen times must fit the tuning slider exactly");
                float daylight = BoardAtmosphere.lighting(settings).daylight();
                if (category.isDuskDawn()) {
                    assertTrue((hour >= 6 && hour <= 6.75f) || (hour >= 17.25f && hour <= 18),
                          "Twilight must use the dawn or sunset window, but was " + hour);
                    assertTrue(daylight > 0 && daylight < 1, "Every twilight choice stays within the transition");
                } else if (category.isFullMoonOrMoonlessOrPitchBack()) {
                    assertTrue(hour >= 20 || hour <= 4, "The night window crosses midnight");
                    assertEquals(0, daylight);
                } else {
                    assertTrue(hour >= 8 && hour <= 17);
                    assertEquals(1, daylight);
                }
                assertEquals(settings, BoardAtmosphere.fromScenario(conditions, false, sample),
                      "Reusing the view's sample must not reroll the time");
            }
            assertTrue(hours.size() > 2, "Each lighting category must offer varied times");
            if (category.isDuskDawn()) {
                assertTrue(hours.stream().anyMatch(value -> value < 12), "Dawn must be reachable");
                assertTrue(hours.stream().anyMatch(value -> value > 12), "Sunset must be reachable");
            }
            if (category.isFullMoonOrMoonlessOrPitchBack()) {
                assertTrue(hours.contains(20f) && hours.contains(0f) && hours.contains(4f));
            }
        }
        for (double invalid : new double[] { -0.1, 1, Double.NaN, Double.POSITIVE_INFINITY }) {
            assertThrows(IllegalArgumentException.class, () -> BoardAtmosphere.fromScenario(conditions, false, invalid));
        }
    }

    @Test
    void everyScenarioWeatherSelectsTheAppropriateEffectsAndClearRemovesThem() {
        PlanetaryConditions conditions = new PlanetaryConditions();
        for (Weather weather : Weather.values()) {
            conditions.setWeather(weather);
            var settings = BoardAtmosphere.fromScenario(conditions, false, 0.5);
            var effects = settings.effects();
            assertEquals(EnumSet.of(Weather.LIGHT_RAIN, Weather.MOD_RAIN, Weather.HEAVY_RAIN, Weather.GUSTING_RAIN,
                        Weather.DOWNPOUR, Weather.SLEET, Weather.ICE_STORM, Weather.LIGHTNING_STORM).contains(weather),
                  effects.rain() > 0, "Rain for " + weather);
            assertEquals(EnumSet.of(Weather.LIGHT_SNOW, Weather.MOD_SNOW, Weather.SNOW_FLURRIES, Weather.HEAVY_SNOW,
                  Weather.SLEET).contains(weather), effects.snow() > 0, "Snow for " + weather);
            assertEquals(weather.isLightHail() || weather.isHeavyHail() || weather.isIceStorm(), effects.hail() > 0,
                  "Hail for " + weather);
            assertEquals(weather.isLightningStorm(), effects.lightning() > 0);
            assertEquals(!weather.isClear(), settings.clouds() > 0);
            assertFalse(AtmospherePreset.CLEAR.settings(0.5).effects().hasParticles());
            assertEquals(0, AtmospherePreset.CLEAR.settings(0.5).effects().lightning());
            assertEquals(weather, conditions.getWeather());
        }
        conditions.setWeather(Weather.LIGHT_RAIN);
        float lightRain = BoardAtmosphere.fromScenario(conditions, false, 0.5).effects().rain();
        conditions.setWeather(Weather.DOWNPOUR);
        assertTrue(BoardAtmosphere.fromScenario(conditions, false, 0.5).effects().rain() > lightRain);
        assertEquals(1, BoardAtmosphere.fromScenario(conditions, false, 0.5).effects().rain(), "Downpour uses full density");
        conditions.setWeather(Weather.LIGHT_SNOW);
        float lightSnow = BoardAtmosphere.fromScenario(conditions, false, 0.5).effects().snow();
        conditions.setWeather(Weather.HEAVY_SNOW);
        assertTrue(BoardAtmosphere.fromScenario(conditions, false, 0.5).effects().snow() > lightSnow);
        assertEquals(1, BoardAtmosphere.fromScenario(conditions, false, 0.5).effects().snow());
        conditions.setWeather(Weather.HEAVY_HAIL);
        assertEquals(1, BoardAtmosphere.fromScenario(conditions, false, 0.5).effects().hail());
    }

    @Test
    void pressureSpaceFogAndEffectiveWindConstrainScenarioWeather() {
        PlanetaryConditions conditions = new PlanetaryConditions();
        conditions.setWeather(Weather.HEAVY_RAIN);
        conditions.setFog(Fog.FOG_HEAVY);
        conditions.setWind(Wind.STORM);
        conditions.setWindDirection(WindDirection.SOUTHWEST);
        conditions.setBlowingSand(BlowingSand.BLOWING_SAND);
        for (Atmosphere pressure : Atmosphere.values()) {
            conditions.setAtmosphere(pressure);
            var settings = BoardAtmosphere.fromScenario(conditions, false, 0.5);
            boolean wet = pressure.isDenserThan(Atmosphere.THIN);
            assertEquals(wet, settings.effects().rain() > 0);
            assertEquals(wet, settings.fog() > 0);
            assertEquals(!pressure.isVacuum(), settings.effects().sand() > 0);
            assertEquals(!pressure.isVacuum(), settings.effects().wind() > 0);
            var space = BoardAtmosphere.fromScenario(conditions, true, 0.5);
            assertFalse(space.effects().hasParticles());
            assertEquals(0, space.clouds());
            assertEquals(0, space.fog());
            assertEquals(0, space.haze());
            assertEquals(0, space.effects().sand());
            assertEquals(0, space.effects().wind());
        }
        conditions.setAtmosphere(Atmosphere.STANDARD);
        assertEquals(60, BoardAtmosphere.fromScenario(conditions, false, 0.5).effects().windDirection());
        conditions.setWind(Wind.LIGHT_GALE);
        assertEquals(0, BoardAtmosphere.fromScenario(conditions, false, 0.5).effects().sand(),
              "Use the game's effective blowing-sand state, not the enabled flag alone");
        conditions.setWindDirection(WindDirection.RANDOM);
        assertEquals(0, BoardAtmosphere.fromScenario(conditions, false, 0.5).effects().wind(),
              "Cloud cover must not introduce wind when the scenario direction is unresolved");
        assertEquals(WindDirection.RANDOM, conditions.getWindDirection(), "Visual drift must not resolve game wind rolls");
        conditions.setWeather(Weather.CLEAR);
        conditions.setFog(Fog.FOG_NONE);
        assertEquals(0, BoardAtmosphere.fromScenario(conditions, false, 0.5).effects().wind());
        conditions.setWind(Wind.STORM);
        var blowingSand = BoardAtmosphere.fromScenario(conditions, false, 0.5);
        assertTrue(blowingSand.effects().sand() > 0);
        assertEquals(BoardAtmosphere.MIN_SAND_FOG, blowingSand.fog(),
              "Blowing sand must keep a minimum ground fog even with fog none");
        assertEquals(0, blowingSand.haze(), "Sand has its own bounded veil and adds no haze");
    }

    @Test
    void higherPressureLowersBothFogLayersWithoutChangingWeatherOrLighting() {
        PlanetaryConditions conditions = new PlanetaryConditions();
        conditions.setLight(Light.DUSK_DAWN);
        conditions.setWeather(Weather.HEAVY_RAIN);
        for (Fog fog : new Fog[] { Fog.FOG_LIGHT, Fog.FOG_HEAVY }) {
            conditions.setFog(fog);
            conditions.setAtmosphere(Atmosphere.STANDARD);
            var standard = BoardAtmosphere.fromScenario(conditions, false, 0.5);
            conditions.setAtmosphere(Atmosphere.HIGH);
            var high = BoardAtmosphere.fromScenario(conditions, false, 0.5);
            conditions.setAtmosphere(Atmosphere.VERY_HIGH);
            var veryHigh = BoardAtmosphere.fromScenario(conditions, false, 0.5);
            assertEquals(BoardAtmosphere.STANDARD_GROUND_LAYER_HEIGHT, standard.groundLayerHeight());
            assertEquals(standard.groundLayerHeight() - 0.75f, high.groundLayerHeight());
            assertEquals(Math.max(BoardAtmosphere.MIN_GROUND_LAYER_HEIGHT, high.groundLayerHeight() - 0.75f),
                  veryHigh.groundLayerHeight());
            assertEquals(BoardAtmosphere.MIN_GROUND_LAYER_HEIGHT, veryHigh.groundLayerHeight());
            for (var compressed : new BoardAtmosphere.Settings[] { high, veryHigh }) {
                assertEquals(standard.fog(), compressed.fog());
                assertEquals(standard.haze(), compressed.haze());
                assertEquals(standard.hour(), compressed.hour());
                assertEquals(standard.exposure(), compressed.exposure());
                assertEquals(standard.clouds(), compressed.clouds());
                assertEquals(standard.effects(), compressed.effects());
            }
        }
    }

    @Test
    void fogAddsCloudCoverWithoutReplacingPrecipitationClouds() {
        PlanetaryConditions conditions = new PlanetaryConditions();
        assertEquals(0, BoardAtmosphere.fromScenario(conditions, false, 0.5).clouds());
        conditions.setFog(Fog.FOG_LIGHT);
        float lightCover = BoardAtmosphere.fromScenario(conditions, false, 0.5).clouds();
        assertTrue(lightCover > 0);
        conditions.setFog(Fog.FOG_HEAVY);
        float heavyCover = BoardAtmosphere.fromScenario(conditions, false, 0.5).clouds();
        assertTrue(heavyCover > lightCover);
        conditions.setWeather(Weather.HEAVY_RAIN);
        conditions.setFog(Fog.FOG_NONE);
        float rainCover = BoardAtmosphere.fromScenario(conditions, false, 0.5).clouds();
        assertTrue(rainCover > heavyCover);
        conditions.setFog(Fog.FOG_HEAVY);
        assertEquals(rainCover, BoardAtmosphere.fromScenario(conditions, false, 0.5).clouds(),
              "Denser precipitation clouds still win over fog clouds");
        conditions.setAtmosphere(Atmosphere.THIN);
        assertEquals(0, BoardAtmosphere.fromScenario(conditions, false, 0.5).clouds(),
              "Thin atmospheres suppress fog and its cloud cover");
    }

    private static BoardAtmosphere.Settings at(float hour) {
        return new BoardAtmosphere.Settings(hour, 0.15f, 0.08f, 2.5f, 0.2f, 0);
    }
}

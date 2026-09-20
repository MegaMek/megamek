/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;

import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.planetaryConditions.BlowingSand;
import megamek.common.planetaryConditions.Fog;
import megamek.common.planetaryConditions.Light;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.planetaryConditions.Weather;
import megamek.common.planetaryConditions.Wind;
import megamek.common.planetaryConditions.WindDirection;
import org.junit.jupiter.api.Test;

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
    void allClockPositionsHaveFiniteDownwardLightingWithoutAShadowCameraPole() {
        for (int quarter = 0; quarter <= 96; quarter++) {
            var light = BoardAtmosphere.lighting(at(quarter / 4f));
            assertEquals(1, light.direction().len(), 0.0001);
            assertTrue(light.direction().z < -0.05f);
            assertTrue(Math.abs(light.direction().y) > 0.05f);
            assertTrue(Float.isFinite(light.direct().r) && Float.isFinite(light.ambient().r));
        }
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
    void weatherPreservesTimeAndExposureWhileCloudsReduceDirectionalContrast() {
        var clear = BoardAtmosphere.Weather.CLEAR.apply(new BoardAtmosphere.Settings(17, 1, 1, 8, 1, 1));
        assertEquals(0, clear.clouds(), "Clear must remove all cloud cover");
        assertEquals(0, clear.fog());
        assertEquals(0, clear.haze());
        assertEquals(BoardAtmosphere.DEFAULTS, BoardAtmosphere.Weather.CLEAR.apply(BoardAtmosphere.DEFAULTS),
              "The initial atmosphere must use the Clear preset");
        var overcast = BoardAtmosphere.Weather.OVERCAST.apply(clear);
        var mist = BoardAtmosphere.Weather.MIST.apply(clear);
        assertEquals(clear.hour(), overcast.hour());
        assertEquals(clear.exposure(), overcast.exposure());
        assertTrue(BoardAtmosphere.lighting(overcast).direct().r < BoardAtmosphere.lighting(clear).direct().r);
        var night = at(0);
        assertTrue(BoardAtmosphere.lighting(BoardAtmosphere.Weather.OVERCAST.apply(night)).direct().b
              < BoardAtmosphere.lighting(night).direct().b, "Cloud cover also dims the moon");
        assertTrue(mist.fogHeight() < overcast.fogHeight() && mist.fog() > overcast.fog());
        var clearNoon = BoardAtmosphere.lighting(new BoardAtmosphere.Settings(12, 0, 0, 2.5f, 0, 0));
        var cloudyNoon = BoardAtmosphere.lighting(new BoardAtmosphere.Settings(12, 1, 0, 2.5f, 0, 0));
        assertTrue(cloudyNoon.direct().r >= clearNoon.direct().r * 0.45f,
              "Maximum cloud cover must preserve a substantial directional component and visible shadows");
        assertTrue(clearNoon.exposureScale(0) > 1.5f, "Neutral daytime exposure must lift the dim LDR scene");
        assertEquals(0.5f, BoardAtmosphere.lighting(at(0)).exposureScale(0), 0.0001,
              "Neutral night exposure must use -1 EV");
    }

    @Test
    void settingsRejectNonFiniteValuesAndBoundShaderInputs() {
        assertThrows(IllegalArgumentException.class,
              () -> new BoardAtmosphere.Settings(Float.NaN, 0, 0, 1, 0, 0));
        var bounded = new BoardAtmosphere.Settings(-1, 2, -1, 0, 3, 8);
        assertEquals(23, bounded.hour());
        assertEquals(1, bounded.clouds());
        assertEquals(0, bounded.fog());
        assertEquals(1.0f, bounded.fogHeight());
        assertEquals(2, bounded.exposure());
        assertThrows(IllegalArgumentException.class,
              () -> new BoardAtmosphere.Effects(0, 0, 0, 0, 0, Float.POSITIVE_INFINITY, 0));
        var effects = new BoardAtmosphere.Effects(2, -1, 3, -4, 5, 6, -60);
        assertEquals(new BoardAtmosphere.Effects(1, 0, 1, 0, 1, 1, 300), effects);
    }

    @Test
    void scenarioLightCategoriesChooseDayTwilightOrReadableNight() {
        PlanetaryConditions conditions = new PlanetaryConditions();
        assertEquals(BoardAtmosphere.DEFAULTS, BoardAtmosphere.fromScenario(conditions, false));
        for (Light light : Light.values()) {
            conditions.setLight(light);
            var settings = BoardAtmosphere.fromScenario(conditions, false);
            float daylight = BoardAtmosphere.lighting(settings).daylight();
            if (light == Light.DUSK) {
                assertTrue(daylight > 0 && daylight < 1, "Dusk must use the twilight transition");
            } else if (light.isDuskOrFullMoonOrMoonlessOrPitchBack()) {
                assertEquals(0, daylight);
                float expectedExposure = switch (light) {
                    case MOONLESS -> -0.3f;
                    case PITCH_BLACK -> -0.6f;
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
    void everyScenarioWeatherSelectsTheAppropriateEffectsAndClearRemovesThem() {
        PlanetaryConditions conditions = new PlanetaryConditions();
        for (Weather weather : Weather.values()) {
            conditions.setWeather(weather);
            var settings = BoardAtmosphere.fromScenario(conditions, false);
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
            assertFalse(BoardAtmosphere.Weather.CLEAR.apply(settings).effects().hasParticles());
            assertEquals(0, BoardAtmosphere.Weather.CLEAR.apply(settings).effects().lightning());
            assertEquals(weather, conditions.getWeather());
        }
        conditions.setWeather(Weather.LIGHT_RAIN);
        float lightRain = BoardAtmosphere.fromScenario(conditions, false).effects().rain();
        conditions.setWeather(Weather.DOWNPOUR);
        assertTrue(BoardAtmosphere.fromScenario(conditions, false).effects().rain() > lightRain);
        assertEquals(1, BoardAtmosphere.fromScenario(conditions, false).effects().rain(), "Downpour uses full density");
        conditions.setWeather(Weather.LIGHT_SNOW);
        float lightSnow = BoardAtmosphere.fromScenario(conditions, false).effects().snow();
        conditions.setWeather(Weather.HEAVY_SNOW);
        assertTrue(BoardAtmosphere.fromScenario(conditions, false).effects().snow() > lightSnow);
        assertEquals(1, BoardAtmosphere.fromScenario(conditions, false).effects().snow());
        conditions.setWeather(Weather.HEAVY_HAIL);
        assertEquals(1, BoardAtmosphere.fromScenario(conditions, false).effects().hail());
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
            var settings = BoardAtmosphere.fromScenario(conditions, false);
            boolean wet = pressure.isDenserThan(Atmosphere.THIN);
            assertEquals(wet, settings.effects().rain() > 0);
            assertEquals(wet, settings.fog() > 0);
            assertEquals(!pressure.isVacuum(), settings.effects().sand() > 0);
            assertEquals(!pressure.isVacuum(), settings.effects().wind() > 0);
            var space = BoardAtmosphere.fromScenario(conditions, true);
            assertFalse(space.effects().hasParticles());
            assertEquals(0, space.clouds());
            assertEquals(0, space.fog());
            assertEquals(0, space.haze());
            assertEquals(0, space.effects().wind());
        }
        conditions.setAtmosphere(Atmosphere.STANDARD);
        assertEquals(60, BoardAtmosphere.fromScenario(conditions, false).effects().windDirection());
        conditions.setWind(Wind.LIGHT_GALE);
        assertEquals(0, BoardAtmosphere.fromScenario(conditions, false).effects().sand(),
              "Use the game's effective blowing-sand state, not the enabled flag alone");
        conditions.setWindDirection(WindDirection.RANDOM);
        assertEquals(0, BoardAtmosphere.fromScenario(conditions, false).effects().wind(),
              "The renderer must not roll or invent the scenario's unresolved wind direction");
    }

    @Test
    void higherPressureLowersBothFogLayersWithoutChangingWeatherOrLighting() {
        PlanetaryConditions conditions = new PlanetaryConditions();
        conditions.setLight(Light.DUSK);
        conditions.setWeather(Weather.HEAVY_RAIN);
        for (Fog fog : new Fog[] { Fog.FOG_LIGHT, Fog.FOG_HEAVY }) {
            conditions.setFog(fog);
            conditions.setAtmosphere(Atmosphere.STANDARD);
            var standard = BoardAtmosphere.fromScenario(conditions, false);
            conditions.setAtmosphere(Atmosphere.HIGH);
            var high = BoardAtmosphere.fromScenario(conditions, false);
            conditions.setAtmosphere(Atmosphere.VERY_HIGH);
            var veryHigh = BoardAtmosphere.fromScenario(conditions, false);
            assertEquals(2.5f, standard.fogHeight());
            assertEquals(standard.fogHeight() - 0.75f, high.fogHeight());
            assertEquals(high.fogHeight() - 0.75f, veryHigh.fogHeight());
            assertEquals(BoardAtmosphere.MIN_FOG_HEIGHT, veryHigh.fogHeight());
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
        assertEquals(0, BoardAtmosphere.fromScenario(conditions, false).clouds());
        conditions.setFog(Fog.FOG_LIGHT);
        assertEquals(0.15f, BoardAtmosphere.fromScenario(conditions, false).clouds());
        conditions.setFog(Fog.FOG_HEAVY);
        assertEquals(0.3f, BoardAtmosphere.fromScenario(conditions, false).clouds());
        conditions.setWeather(Weather.HEAVY_RAIN);
        assertEquals(0.85f, BoardAtmosphere.fromScenario(conditions, false).clouds(),
              "Denser precipitation clouds still win over fog clouds");
        conditions.setAtmosphere(Atmosphere.THIN);
        assertEquals(0, BoardAtmosphere.fromScenario(conditions, false).clouds(),
              "Thin atmospheres suppress fog and its cloud cover");
    }

    private static BoardAtmosphere.Settings at(float hour) {
        return new BoardAtmosphere.Settings(hour, 0.15f, 0.08f, 2.5f, 0.2f, 0);
    }
}

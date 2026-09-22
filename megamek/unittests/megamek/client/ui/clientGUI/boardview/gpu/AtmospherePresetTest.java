/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.planetaryConditions.AtmosphericTaint;
import megamek.common.planetaryConditions.Light;
import megamek.common.planetaryConditions.PlanetaryConditions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class AtmospherePresetTest {
    @ParameterizedTest
    @EnumSource(AtmospherePreset.class)
    void presetsUseScenarioMappingAndOwnTheirConditions(AtmospherePreset preset) {
        var conditions = preset.conditions();
        assertNotSame(conditions, preset.conditions());
        assertEquals(PlanetaryConditions.setTempFromWeather(conditions.getWeather(), conditions.getTemperature()),
              conditions.getTemperature());
        assertEquals(PlanetaryConditions.setWindFromWeather(conditions.getWeather(), conditions.getWind()), conditions.getWind());
        for (double sample : new double[] { 0, 0.25, 0.5, 0.75, Math.nextDown(1.0) }) {
            double scenarioSample = switch (preset) {
                case DAWN -> sample * 0.5;
                case DUSK -> Math.min(Math.nextDown(1.0), 0.5 + sample * 0.5);
                default -> sample;
            };
            assertEquals(BoardAtmosphere.fromScenario(conditions, false, scenarioSample), preset.settings(sample));
            assertEquals(preset.settings(sample), preset.settings(sample), "Repeated clicks keep this window's sampled time");
        }
    }

    @Test
    void nightPresetsControlBothTheSourceAndItsExposure() {
        var moon = AtmospherePreset.FULL_MOON.settings(0.5);
        var moonless = AtmospherePreset.MOONLESS.settings(0.5);
        var dark = AtmospherePreset.PITCH_BLACK.settings(0.5);
        assertTrue(moon.moonlight());
        assertTrue(BoardAtmosphere.lighting(moon).hasDirectLight());
        assertFalse(moonless.moonlight());
        assertFalse(dark.moonlight());
        assertFalse(BoardAtmosphere.lighting(moonless).hasDirectLight());
        assertFalse(BoardAtmosphere.lighting(dark).hasDirectLight());
        assertEquals(moon.hour(), moonless.hour());
        assertEquals(moonless.hour(), dark.hour());
        assertTrue(dark.exposure() < moonless.exposure() && moonless.exposure() < moon.exposure());
        for (int sample = 0; sample < 128; sample++) {
            float dawn = AtmospherePreset.DAWN.settings(sample / 128.0).hour();
            float dusk = AtmospherePreset.DUSK.settings(sample / 128.0).hour();
            assertTrue(dawn >= 6 && dawn <= 6.75f);
            assertTrue(dusk >= 17.25f && dusk <= 18);
        }
    }

    @Test
    void weatherCombinationsProduceTheirIntendedEffectsAndLunarHasNoAir() {
        var rain = AtmospherePreset.RAIN_STORM.settings(0.5);
        assertTrue(rain.effects().rain() > 0 && rain.effects().lightning() > 0 && rain.effects().wind() > 0);
        assertTrue(BoardAtmosphere.wetness(rain) > 0);
        var snow = AtmospherePreset.SNOW_STORM.settings(0.5);
        assertEquals(1, snow.effects().snow());
        assertTrue(snow.temperature() < 0 && snow.effects().wind() > 0);
        assertEquals(0, BoardAtmosphere.wetness(snow));
        var sand = AtmospherePreset.SAND_STORM.settings(0.5);
        assertTrue(sand.effects().sand() > 0 && sand.effects().wind() > 0);
        assertEquals(0, sand.effects().rain());
        assertEquals(BoardAtmosphere.MIN_SAND_FOG, sand.fog());
        assertEquals(0, sand.haze());
        var lunar = AtmospherePreset.LUNAR.settings(0.5);
        assertEquals(Atmosphere.VACUUM, lunar.pressure());
        assertEquals(0.16f, lunar.gravity());
        assertEquals(0, lunar.clouds());
        assertEquals(0, lunar.fog());
        assertEquals(0, lunar.haze());
        assertEquals(BoardAtmosphere.Effects.NONE, lunar.effects());
    }

    @Test
    void newlyExposedVariablesFollowScenarioUntilIndividuallyOverridden() {
        var conditions = new PlanetaryConditions();
        conditions.setLight(Light.FULL_MOON);
        var original = BoardAtmosphere.fromScenario(conditions, false, 0.5);
        var manual = new BoardAtmosphere.Settings(original.hour(), 0, 0, original.groundLayerHeight(), 0, -1,
              original.effects(), Atmosphere.THIN, -20, false, AtmosphericTaint.TOXIC_CAUSTIC, 0.5f);
        conditions.setAtmosphere(Atmosphere.HIGH);
        conditions.setTemperature(35);
        conditions.setAtmosphericTaint(AtmosphericTaint.TAINTED_FLAME);
        conditions.setLight(Light.MOONLESS);
        conditions.setGravity(2);
        var next = BoardAtmosphere.fromScenario(conditions, false, 0.5);
        assertEquals(next, BoardAtmosphere.followScenario(original, original, next));
        var preserved = BoardAtmosphere.followScenario(manual, original, next);
        assertEquals(manual.pressure(), preserved.pressure());
        assertEquals(manual.temperature(), preserved.temperature());
        assertEquals(manual.taint(), preserved.taint());
        assertEquals(manual.moonlight(), preserved.moonlight());
        assertEquals(manual.gravity(), preserved.gravity());
        assertEquals(manual.exposure(), preserved.exposure());
        assertEquals(Atmosphere.HIGH, conditions.getAtmosphere(), "Preview overrides must not change the game");
    }
}

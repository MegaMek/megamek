/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Vector3;
import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.planetaryConditions.Weather;
import megamek.common.planetaryConditions.Wind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GpuCloudsTest {
    @Test
    void coverScalesPatchOpacityBetweenTunableLimits() {
        var options = GpuAtmosphere.Options.DEFAULTS;
        assertEquals(GpuClouds.MIN_SHADOW_STRENGTH, options.minCloudShadow());
        assertEquals(GpuClouds.MAX_SHADOW_STRENGTH, options.maxCloudShadow());
        assertEquals(0, options.cloudShadowStrength(0));
        float previous = options.minCloudShadow();
        for (int percent = 1; percent <= 100; percent++) {
            float strength = options.cloudShadowStrength(percent / 100f);
            assertTrue(strength > previous && strength <= options.maxCloudShadow());
            previous = strength;
        }
        assertEquals(options.maxCloudShadow(), previous);
        assertEquals(0.4f, new GpuAtmosphere.Options(0.5f, false, 0.1f, 0.7f).cloudShadowStrength(0.5f), 0.0001f);
        var disabled = new GpuAtmosphere.Options(0.5f, false, 0, 0);
        assertEquals(0, disabled.cloudShadowStrength(1));
    }

    @Test
    void shadowControlsKeepABoundedOrderedRange() {
        var clamped = new GpuAtmosphere.Options(3, true, -1, 2);
        assertEquals(2, clamped.rays());
        assertEquals(0, clamped.minCloudShadow());
        assertEquals(1, clamped.maxCloudShadow());
        var reversed = new GpuAtmosphere.Options(0.5f, false, 0.8f, 0.2f);
        assertEquals(reversed.minCloudShadow(), reversed.maxCloudShadow());
        assertThrows(IllegalArgumentException.class, () -> new GpuAtmosphere.Options(0.5f, false, Float.NaN, 1));
        assertThrows(IllegalArgumentException.class, () -> new GpuAtmosphere.Options(0.5f, false, 0, Float.POSITIVE_INFINITY));
    }

    @Test
    void sunGlareCanBeDisabledAndRejectsInvalidInput() {
        assertEquals(0.35f, GpuAtmosphere.Options.DEFAULTS.sunGlare());
        assertEquals(0, new GpuAtmosphere.Options(0, false, 0, 1, -1).sunGlare());
        assertEquals(1, new GpuAtmosphere.Options(0, false, 0, 1, 2).sunGlare());
        assertThrows(IllegalArgumentException.class, () -> new GpuAtmosphere.Options(0, false, 0, 1, Float.NaN));
        assertThrows(IllegalArgumentException.class,
              () -> new GpuAtmosphere.Options(0, false, 0, 1, Float.POSITIVE_INFINITY));
    }

    @Test
    void scenarioClockAndWindChangesFollowTheGameWhileVisualOverridesSurvive() {
        var previous = BoardAtmosphere.DEFAULTS;
        var visual = new BoardAtmosphere.Settings(17, 0.6f, 0, 2.5f, 0, 0);
        var next = new BoardAtmosphere.Settings(8, 0, 0, 2.5f, 0, 0,
              new BoardAtmosphere.Effects(0, 0, 0, 0, 0, 0.8f, 120));
        var followed = BoardAtmosphere.followScenario(visual, previous, next);
        assertEquals(next.hour(), BoardAtmosphere.followScenario(previous, previous, next).hour());
        assertEquals(visual.hour(), followed.hour());
        assertEquals(visual.clouds(), followed.clouds());
        assertEquals(next.effects(), followed.effects());
        var override = new BoardAtmosphere.Settings(17, 0.6f, 0, 2.5f, 0, 0,
              new BoardAtmosphere.Effects(0, 0, 0, 0, 0, 0.4f, 90));
        assertEquals(override.effects(), BoardAtmosphere.followScenario(override, previous, next).effects());
        var vacuum = new BoardAtmosphere.Settings(13, 0, 0, 2.5f, 0, 0,
              BoardAtmosphere.Effects.NONE, Atmosphere.VACUUM);
        assertEquals(0, BoardAtmosphere.followScenario(override, previous, vacuum).clouds());
    }

    @Test
    void cloudCoverPreservesWindWhileCloudsStillDrift() {
        var calm = BoardAtmosphere.DEFAULTS;
        for (float cover : new float[] { 0.0001f, 0.35f, 1 }) {
            var cloudy = new BoardAtmosphere.Settings(13, cover, 0, 2.5f, 0, 0);
            assertEquals(0, cloudy.effects().wind());
            var motion = new GpuClouds.Motion();
            motion.advance(cloudy.effects(), 0.1f);
            assertTrue(motion.offset.y > 0, "Even minimal cloud cover moves with initially calm wind");
            var breezy = new BoardAtmosphere.Settings(13, cover, 0, 2.5f, 0, 0,
                  new BoardAtmosphere.Effects(0, 0, 0, 0, 0, 0.1f, 120));
            assertEquals(0.1f, breezy.effects().wind());
            assertEquals(120, breezy.effects().windDirection());
            assertEquals(calm, BoardAtmosphere.followScenario(cloudy, cloudy, calm));
        }
        PlanetaryConditions conditions = new PlanetaryConditions();
        conditions.setWind(Wind.CALM);
        conditions.setWeather(Weather.HEAVY_RAIN);
        assertEquals(0, BoardAtmosphere.fromScenario(conditions, false, 0.5).effects().wind());
        assertEquals(Wind.CALM, conditions.getWind());
        assertEquals(0, BoardAtmosphere.fromScenario(conditions, true, 0.5).effects().wind());
        conditions.setAtmosphere(Atmosphere.TRACE);
        assertEquals(0, BoardAtmosphere.fromScenario(conditions, false, 0.5).effects().wind());
    }

    @Test
    void vacuumPressureSuppressesWeatherEvenWhenManualControlsRequestIt() {
        PlanetaryConditions conditions = new PlanetaryConditions();
        for (boolean space : new boolean[] { false, true }) {
            conditions.setAtmosphere(space ? Atmosphere.STANDARD : Atmosphere.VACUUM);
            var initial = BoardAtmosphere.fromScenario(conditions, space, 0.5);
            assertEquals(Atmosphere.VACUUM, initial.pressure());
            var manual = new BoardAtmosphere.Settings(13, 1, 1, 4, 1, 0,
                  new BoardAtmosphere.Effects(1, 1, 1, 1, 1, 1, 90), initial.pressure());
            assertEquals(0, manual.clouds());
            assertEquals(BoardAtmosphere.Effects.NONE, manual.effects());
        }
    }

    @Test
    void pressureAndWeatherSelectWispsLayersAndDeepStormClouds() {
        var fair = new BoardAtmosphere.Settings(13, 0.6f, 0, 2.5f, 0, 0);
        var rain = new BoardAtmosphere.Settings(13, 0.6f, 0, 2.5f, 0, 0,
              new BoardAtmosphere.Effects(1, 0, 0, 0, 0.8f, 0, 0));
        var snow = new BoardAtmosphere.Settings(13, 0.6f, 0, 2.5f, 0, 0,
              new BoardAtmosphere.Effects(0, 1, 0, 0, 0, 0, 0));
        var thin = new BoardAtmosphere.Settings(13, 0.6f, 0, 2.5f, 0, 0,
              BoardAtmosphere.Effects.NONE, Atmosphere.THIN);
        var trace = new BoardAtmosphere.Settings(13, 1, 0, 2.5f, 0, 0,
              BoardAtmosphere.Effects.NONE, Atmosphere.TRACE);
        var clearProfile = BoardAtmosphere.clouds(fair);
        var storm = BoardAtmosphere.clouds(rain);
        assertTrue(storm.thickness() > clearProfile.thickness());
        assertTrue(storm.density() > clearProfile.density());
        assertTrue(storm.altitude() < clearProfile.altitude());
        assertTrue(BoardAtmosphere.clouds(snow).stratus() > clearProfile.stratus());
        assertTrue(BoardAtmosphere.clouds(thin).density() < clearProfile.density());
        assertTrue(BoardAtmosphere.clouds(thin).thickness() < clearProfile.thickness(),
              "Thin-air wisps have a shorter optical path than the standard cloud deck");
        assertTrue(BoardAtmosphere.clouds(thin).scattering() < clearProfile.scattering());
        assertEquals(0, trace.clouds());
    }

    @ParameterizedTest
    @CsvSource({ "0, 0.054", "0.1, 0.0666", "0.5, 0.117", "1, 0.18" })
    void windIsIntegratedContinuouslyWithMinimumCloudDrift(float wind, float distancePerSecond) {
        var motion = new GpuClouds.Motion();
        var north = new BoardAtmosphere.Effects(0, 0, 0, 0, 0, wind, 0);
        for (int frame = 0; frame < 60; frame++) { motion.advance(north, 1f / 60); }
        assertEquals(0, motion.offset.x, 0.0001f);
        assertEquals(distancePerSecond, motion.offset.y, 0.0001f);
        Vector3 before = motion.offset.cpy();
        var east = new BoardAtmosphere.Effects(0, 0, 0, 0, 0, wind, 90);
        motion.advance(east, 0);
        assertEquals(before, motion.offset, "A wind change must not teleport the density field");
        motion.advance(east, 0.1f);
        assertTrue(motion.offset.x > before.x);
        assertEquals(before.y, motion.offset.y, 0.0001f);
        before.set(motion.offset);
        motion.advance(BoardAtmosphere.Effects.NONE, 0.1f);
        assertEquals(before.x, motion.offset.x);
        assertEquals(before.y + 0.0054f, motion.offset.y, 0.0001f);
        assertTrue(motion.offset.z > before.z, "Calm clouds still evolve slowly");
        before.set(motion.offset);
        motion.advance(east, Float.NaN);
        motion.advance(east, -1);
        assertEquals(before, motion.offset);
    }
}

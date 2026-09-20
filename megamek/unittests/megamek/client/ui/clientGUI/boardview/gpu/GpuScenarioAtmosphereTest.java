/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.SwingUtilities;

import megamek.common.planetaryConditions.Fog;
import megamek.common.planetaryConditions.Light;
import megamek.common.planetaryConditions.Weather;
import megamek.common.planetaryConditions.Wind;
import megamek.common.planetaryConditions.WindDirection;
import org.junit.jupiter.api.Test;

class GpuScenarioAtmosphereTest {
    @Test
    void sourcePublishesImmutableEffectiveConditionsWithoutChangingTheScenario() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            SwingUtilities.invokeAndWait(() -> {
                var conditions = fixture.game.getPlanetaryConditions();
                conditions.setLight(Light.FULL_MOON);
                conditions.setWeather(Weather.HEAVY_SNOW);
                conditions.setFog(Fog.FOG_HEAVY);
                conditions.setWind(Wind.STRONG_GALE);
                conditions.setWindDirection(WindDirection.NORTH);
                fixture.source.refresh();
            });
            var initial = fixture.source.takeFrame().scenarioAtmosphere();
            assertEquals(0, initial.hour());
            assertTrue(initial.effects().snow() > 0);
            assertTrue(initial.effects().wind() > 0);
            assertTrue(initial.fog() > 0 && initial.haze() > 0);
            SwingUtilities.invokeAndWait(() -> {
                var conditions = fixture.game.getPlanetaryConditions();
                assertEquals(Light.FULL_MOON, conditions.getLight());
                assertEquals(Weather.HEAVY_SNOW, conditions.getWeather());
                assertEquals(Fog.FOG_HEAVY, conditions.getFog());
                conditions.setFog(Fog.FOG_LIGHT);
                fixture.source.refresh();
            });
            var lightFog = fixture.source.takeFrame().scenarioAtmosphere();
            assertTrue(lightFog.fog() > 0 && lightFog.fog() < initial.fog());
            assertTrue(lightFog.haze() > 0 && lightFog.haze() < initial.haze());
            assertEquals(initial.hour(), lightFog.hour(), "Fog must preserve scenario lighting");
            assertEquals(initial.exposure(), lightFog.exposure());
            assertEquals(initial.clouds(), lightFog.clouds());
            assertEquals(initial.effects(), lightFog.effects(), "Fog must preserve precipitation and wind");
            SwingUtilities.invokeAndWait(() -> {
                var conditions = fixture.game.getPlanetaryConditions();
                assertEquals(Fog.FOG_LIGHT, conditions.getFog());
                conditions.setLight(Light.DAY);
                conditions.setWeather(Weather.CLEAR);
                conditions.setFog(Fog.FOG_NONE);
                fixture.source.refresh();
            });
            var changed = fixture.source.takeFrame().scenarioAtmosphere();
            assertEquals(13, changed.hour());
            assertEquals(0, changed.effects().snow());
            assertEquals(0, changed.fog());
            assertEquals(0, changed.haze());
            assertEquals(0, initial.hour(), "Published snapshots must not share mutable planetary conditions");
            assertTrue(initial.effects().snow() > 0);
            assertTrue(initial.fog() > 0 && initial.haze() > 0);
        }
    }
}

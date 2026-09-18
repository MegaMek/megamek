/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.SwingUtilities;

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
                conditions.setWind(Wind.STRONG_GALE);
                conditions.setWindDirection(WindDirection.NORTH);
                fixture.source.refresh();
            });
            var initial = fixture.source.takeFrame().scenarioAtmosphere();
            assertEquals(0, initial.hour());
            assertTrue(initial.effects().snow() > 0);
            assertTrue(initial.effects().wind() > 0);
            SwingUtilities.invokeAndWait(() -> {
                var conditions = fixture.game.getPlanetaryConditions();
                assertEquals(Light.FULL_MOON, conditions.getLight());
                assertEquals(Weather.HEAVY_SNOW, conditions.getWeather());
                conditions.setLight(Light.DAY);
                conditions.setWeather(Weather.CLEAR);
                fixture.source.refresh();
            });
            var changed = fixture.source.takeFrame().scenarioAtmosphere();
            assertEquals(13, changed.hour());
            assertEquals(0, changed.effects().snow());
            assertEquals(0, initial.hour(), "Published snapshots must not share mutable planetary conditions");
            assertTrue(initial.effects().snow() > 0);
        }
    }
}

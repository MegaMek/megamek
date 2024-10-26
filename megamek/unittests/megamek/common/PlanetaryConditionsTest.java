/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import megamek.common.planetaryconditions.Atmosphere;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.common.planetaryconditions.Wind;

class PlanetaryConditionsTest {

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @Test
    void testWhyDoomed() {
        Game mockGame = mock(Game.class);
        Board mockBoard = mock(Board.class);
        Hex mockHex = mock(Hex.class);
        Coords mockCoords = mock(Coords.class);

        // Trace atmosphere - Entity doomed in vacuum/trace atmosphere
        PlanetaryConditions planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setAtmosphere(Atmosphere.TRACE);
        when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        Entity mockEntity = mock(Infantry.class);
        when(mockEntity.doomedInVacuum()).thenReturn(true);
        assertEquals("vacuum", planetaryConditions.whyDoomed(mockEntity, mockGame));
        reset(mockEntity, mockGame);

        // Trace atmosphere - Entity not doomed in vacuum/trace atmosphere
        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setAtmosphere(Atmosphere.TRACE);
        when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        mockEntity = mock(Infantry.class);
        when(mockEntity.isConventionalInfantry()).thenReturn(true);
        when(mockEntity.doomedInVacuum()).thenReturn(false);
        assertNull(planetaryConditions.whyDoomed(mockEntity, mockGame));
        reset(mockEntity, mockGame);

        // F4 Tornado - Entity is a mek (not doomed)
        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setWind(Wind.TORNADO_F4);
        mockEntity = mock(Mek.class);
        when(mockEntity.getMovementMode()).thenReturn(EntityMovementMode.BIPED);
        when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        assertNull(planetaryConditions.whyDoomed(mockEntity, mockGame));
        reset(mockEntity, mockGame);

        // F4 Tornado - Entity is not a mek (doomed)
        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setWind(Wind.TORNADO_F4);
        mockEntity = mock(Infantry.class);
        when(mockEntity.isConventionalInfantry()).thenReturn(true);
        when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        assertEquals("tornado", planetaryConditions.whyDoomed(mockEntity, mockGame));
        reset(mockEntity, mockGame);

        // F1-3 Tornado - Entity movement mode is hover (doomed)
        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setWind(Wind.TORNADO_F1_TO_F3);
        mockEntity = mock(Tank.class);
        when(mockEntity.getMovementMode()).thenReturn(EntityMovementMode.HOVER);
        when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        assertEquals("tornado", planetaryConditions.whyDoomed(mockEntity, mockGame));
        reset(mockEntity, mockGame);

        // F1-3 Tornado - Entity movement mode is WiGE (doomed)
        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setWind(Wind.TORNADO_F1_TO_F3);
        mockEntity = mock(Tank.class);
        when(mockEntity.getMovementMode()).thenReturn(EntityMovementMode.WIGE);
        when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        assertEquals("tornado", planetaryConditions.whyDoomed(mockEntity, mockGame));
        reset(mockEntity, mockGame);

        // F1-3 Tornado - Entity movement mode is VTOL (doomed)
        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setWind(Wind.TORNADO_F1_TO_F3);
        mockEntity = mock(VTOL.class);
        when(mockEntity.getMovementMode()).thenReturn(EntityMovementMode.VTOL);
        when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        assertEquals("tornado", planetaryConditions.whyDoomed(mockEntity, mockGame));
        reset(mockEntity, mockGame);

        // F1-3 Tornado - Entity is regular infantry (doomed)
        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setWind(Wind.TORNADO_F1_TO_F3);
        mockEntity = mock(Infantry.class);
        when(mockEntity.isConventionalInfantry()).thenReturn(true);
        when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        assertEquals("tornado", planetaryConditions.whyDoomed(mockEntity, mockGame));
        reset(mockEntity, mockGame);

        // F1-3 Tornado - Entity is battle armor infantry (not doomed)
        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setWind(Wind.TORNADO_F1_TO_F3);
        mockEntity = mock(BattleArmor.class);
        when(mockEntity.getMovementMode()).thenReturn(EntityMovementMode.INF_LEG);
        when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        assertNull(planetaryConditions.whyDoomed(mockEntity, mockGame));
        reset(mockEntity, mockGame);

        // Storm - Entity is regular infantry (doomed)
        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setWind(Wind.STORM);
        mockEntity = mock(Infantry.class);
        when(mockEntity.isConventionalInfantry()).thenReturn(true);
        when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        assertEquals("storm", planetaryConditions.whyDoomed(mockEntity, mockGame));
        reset(mockEntity, mockGame);

        // Storm - Entity is battle armor infantry (not doomed)
        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setWind(Wind.STORM);
        mockEntity = mock(BattleArmor.class);
        when(mockEntity.getMovementMode()).thenReturn(EntityMovementMode.INF_LEG);
        when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        assertNull(planetaryConditions.whyDoomed(mockEntity, mockGame));
        reset(mockEntity, mockGame);

        // Extreme temperature - Doomed in extreme temperature, but sheltered in
        // building (not doomed)
        // FIXME: This test is really coupled with Compute.isInBuilding()
        // implementation. It would be nice if I
        // could mock a static class somehow and abstract the whole thing.

        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setTemperature(100);
        when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        when(mockGame.getBoard()).thenReturn(mockBoard);
        when(mockBoard.getHex(any())).thenReturn(mockHex);
        when(mockHex.containsTerrain(Terrains.BLDG_ELEV)).thenReturn(true);
        when(mockHex.containsTerrain(Terrains.BUILDING)).thenReturn(true);
        when(mockHex.terrainLevel(Terrains.BLDG_ELEV)).thenReturn(2);
        mockEntity = mock(Infantry.class);
        when(mockEntity.isConventionalInfantry()).thenReturn(true);
        when(mockEntity.doomedInExtremeTemp()).thenReturn(true);
        when(mockEntity.getPosition()).thenReturn(mockCoords);
        when(mockEntity.getElevation()).thenReturn(1);
        assertNull(planetaryConditions.whyDoomed(mockEntity, mockGame));
        reset(mockEntity, mockGame, mockBoard, mockHex);

        // Extreme temperature - Doomed in extreme temperature (doomed)

        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setTemperature(100);
        when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        mockEntity = mock(Infantry.class);
        when(mockEntity.isConventionalInfantry()).thenReturn(true);
        when(mockEntity.doomedInExtremeTemp()).thenReturn(true);
        assertEquals("extreme temperature", planetaryConditions.whyDoomed(mockEntity, mockGame));
        reset(mockEntity, mockGame);
    }

    @Test
    void testIsExtremeTemperature() {
        // Extreme temperature - Heat
        PlanetaryConditions planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setTemperature(51);
        assertTrue(planetaryConditions.isExtremeTemperature());

        // Extreme temperature - Cold
        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setTemperature(-31);
        assertTrue(planetaryConditions.isExtremeTemperature());

        // Not extreme temperature
        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setTemperature(25);
        assertFalse(planetaryConditions.isExtremeTemperature());
    }

    @Test
    void testGetTemperatureDisplayableName() {
        // Extreme Heat
        assertEquals("51 (Extreme Heat)", PlanetaryConditions.getTemperatureDisplayableName(51));

        // Extreme Cold
        assertEquals("-31 (Extreme Cold)", PlanetaryConditions.getTemperatureDisplayableName(-31));

        // Regular temperature
        assertEquals("25", PlanetaryConditions.getTemperatureDisplayableName(25));
    }
}

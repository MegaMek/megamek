/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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

import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PlanetaryConditionsTest {

    @Test
    public void testWhyDoomed() {
        Game mockGame = mock(Game.class);
        Board mockBoard = mock(Board.class);
        Hex mockHex = mock(Hex.class);
        Coords mockCoords = mock(Coords.class);

        // Trace atmosphere - Entity doomed in vacuum/trace atmosphere
        PlanetaryConditions planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setAtmosphere(PlanetaryConditions.ATMO_TRACE);
        when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        Entity mockEntity = mock(Infantry.class);
        when(mockEntity.doomedInVacuum()).thenReturn(true);
        assertEquals("vacuum", planetaryConditions.whyDoomed(mockEntity, mockGame));
        reset(mockEntity, mockGame);

        // Trace atmosphere - Entity not doomed in vacuum/trace atmosphere
        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setAtmosphere(PlanetaryConditions.ATMO_TRACE);
        when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        mockEntity = mock(Infantry.class);
        when(mockEntity.doomedInVacuum()).thenReturn(false);
        assertNull(planetaryConditions.whyDoomed(mockEntity, mockGame));
        reset(mockEntity, mockGame);

        // F4 Tornado - Entity is a mech (not doomed)
        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setWindStrength(PlanetaryConditions.WI_TORNADO_F4);
        mockEntity = mock(Mech.class);
        when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        assertNull(planetaryConditions.whyDoomed(mockEntity, mockGame));
        reset(mockEntity, mockGame);

        // F4 Tornado - Entity is not a mech (doomed)
        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setWindStrength(PlanetaryConditions.WI_TORNADO_F4);
        mockEntity = mock(Infantry.class);
        when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        assertEquals("tornado", planetaryConditions.whyDoomed(mockEntity, mockGame));
        reset(mockEntity, mockGame);

        // F1-3 Tornado - Entity movement mode is hover (doomed)
        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setWindStrength(PlanetaryConditions.WI_TORNADO_F13);
        mockEntity = mock(Tank.class);
        when(mockEntity.getMovementMode()).thenReturn(EntityMovementMode.HOVER);
        when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        assertEquals("tornado", planetaryConditions.whyDoomed(mockEntity, mockGame));
        reset(mockEntity, mockGame);

        // F1-3 Tornado - Entity movement mode is WIGE (doomed)
        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setWindStrength(PlanetaryConditions.WI_TORNADO_F13);
        mockEntity = mock(Tank.class);
        when(mockEntity.getMovementMode()).thenReturn(EntityMovementMode.WIGE);
        when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        assertEquals("tornado", planetaryConditions.whyDoomed(mockEntity, mockGame));
        reset(mockEntity, mockGame);

        // F1-3 Tornado - Entity movement mode is VTOL (doomed)
        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setWindStrength(PlanetaryConditions.WI_TORNADO_F13);
        mockEntity = mock(VTOL.class);
        when(mockEntity.getMovementMode()).thenReturn(EntityMovementMode.VTOL);
        when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        assertEquals("tornado", planetaryConditions.whyDoomed(mockEntity, mockGame));
        reset(mockEntity, mockGame);

        // F1-3 Tornado - Entity is regular infantry (doomed)
        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setWindStrength(PlanetaryConditions.WI_TORNADO_F13);
        mockEntity = mock(Infantry.class);
        when(mockEntity.isConventionalInfantry()).thenReturn(true);
        when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        assertEquals("tornado", planetaryConditions.whyDoomed(mockEntity, mockGame));
        reset(mockEntity, mockGame);

        // F1-3 Tornado - Entity is battle armor infantry (not doomed)
        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setWindStrength(PlanetaryConditions.WI_TORNADO_F13);
        mockEntity = mock(BattleArmor.class);
        when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        assertNull(planetaryConditions.whyDoomed(mockEntity, mockGame));
        reset(mockEntity, mockGame);

        // Storm - Entity is regular infantry (doomed)
        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setWindStrength(PlanetaryConditions.WI_STORM);
        mockEntity = mock(Infantry.class);
        when(mockEntity.isConventionalInfantry()).thenReturn(true);
        when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        assertEquals("storm", planetaryConditions.whyDoomed(mockEntity, mockGame));
        reset(mockEntity, mockGame);

        // Storm - Entity is battle armor infantry (not doomed)
        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setWindStrength(PlanetaryConditions.WI_STORM);
        mockEntity = mock(BattleArmor.class);
        when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        assertNull(planetaryConditions.whyDoomed(mockEntity, mockGame));
        reset(mockEntity, mockGame);

        // Extreme temperature - Doomed in extreme temperature, but sheltered in building (not doomed)
        // FIXME: This test is really coupled with Compute.isInBuilding() implementation. It would be nice if I
        //  could mock a static class somehow and abstract the whole thing.
        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setTemperature(100);
        when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        when(mockGame.getBoard()).thenReturn(mockBoard);
        when(mockBoard.getHex(any())).thenReturn(mockHex);
        when(mockHex.containsTerrain(Terrains.BLDG_ELEV)).thenReturn(true);
        when(mockHex.containsTerrain(Terrains.BUILDING)).thenReturn(true);
        when(mockHex.terrainLevel(Terrains.BLDG_ELEV)).thenReturn(2);
        mockEntity = mock(Infantry.class);
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
        when(mockEntity.doomedInExtremeTemp()).thenReturn(true);
        assertEquals("extreme temperature", planetaryConditions.whyDoomed(mockEntity, mockGame));
        reset(mockEntity, mockGame);
    }

    @Test
    public void testIsExtremeTemperature() {
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
    public void testGetTemperatureDisplayableName() {
        // Extreme Heat
        assertEquals("51 (Extreme Heat)", PlanetaryConditions.getTemperatureDisplayableName(51));

        // Extreme Cold
        assertEquals("-31 (Extreme Cold)", PlanetaryConditions.getTemperatureDisplayableName(-31));

        // Regular temperature
        assertEquals("25", PlanetaryConditions.getTemperatureDisplayableName(25));
    }
}

package megamek.common;

import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

public class PlanetaryConditionsTest {

    @Test
    public void testWhyDoomed() {
        Game mockGame = Mockito.mock(Game.class);
        Entity mockEntity;
        PlanetaryConditions planetaryConditions;
        Board mockBoard = Mockito.mock(Board.class);
        IHex mockHex = Mockito.mock(Hex.class);
        Coords mockCoords = Mockito.mock(Coords.class);

        // Trace atmosphere - Entity doomed in vacuum/trace atmosphere

        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setAtmosphere(PlanetaryConditions.ATMO_TRACE);
        Mockito.when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        mockEntity = Mockito.mock(Infantry.class);
        Mockito.when(mockEntity.doomedInVacuum()).thenReturn(true);
        assertEquals("vacuum", planetaryConditions.whyDoomed(mockEntity, mockGame));
        Mockito.reset(mockEntity, mockGame);

        // Trace atmosphere - Entity not doomed in vacuum/trace atmosphere

        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setAtmosphere(PlanetaryConditions.ATMO_TRACE);
        Mockito.when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        mockEntity = Mockito.mock(Infantry.class);
        Mockito.when(mockEntity.doomedInVacuum()).thenReturn(false);
        assertNull(planetaryConditions.whyDoomed(mockEntity, mockGame));
        Mockito.reset(mockEntity, mockGame);

        // F4 Tornado - Entity is a mech (not doomed)

        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setWindStrength(PlanetaryConditions.WI_TORNADO_F4);
        mockEntity = Mockito.mock(Mech.class);
        Mockito.when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        assertNull(planetaryConditions.whyDoomed(mockEntity, mockGame));
        Mockito.reset(mockEntity, mockGame);


        // F4 Tornado - Entity is not a mech (doomed)

        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setWindStrength(PlanetaryConditions.WI_TORNADO_F4);
        mockEntity = Mockito.mock(Infantry.class);
        Mockito.when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        assertEquals("tornado", planetaryConditions.whyDoomed(mockEntity, mockGame));
        Mockito.reset(mockEntity, mockGame);

        // F1-3 Tornado - Entity movement mode is hover (doomed)

        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setWindStrength(PlanetaryConditions.WI_TORNADO_F13);
        mockEntity = Mockito.mock(Tank.class);
        Mockito.when(mockEntity.getMovementMode()).thenReturn(EntityMovementMode.HOVER);
        Mockito.when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        assertEquals("tornado", planetaryConditions.whyDoomed(mockEntity, mockGame));
        Mockito.reset(mockEntity, mockGame);

        // F1-3 Tornado - Entity movement mode is WIGE (doomed)

        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setWindStrength(PlanetaryConditions.WI_TORNADO_F13);
        mockEntity = Mockito.mock(Tank.class);
        Mockito.when(mockEntity.getMovementMode()).thenReturn(EntityMovementMode.WIGE);
        Mockito.when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        assertEquals("tornado", planetaryConditions.whyDoomed(mockEntity, mockGame));
        Mockito.reset(mockEntity, mockGame);

        // F1-3 Tornado - Entity movement mode is VTOL (doomed)

        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setWindStrength(PlanetaryConditions.WI_TORNADO_F13);
        mockEntity = Mockito.mock(VTOL.class);
        Mockito.when(mockEntity.getMovementMode()).thenReturn(EntityMovementMode.VTOL);
        Mockito.when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        assertEquals("tornado", planetaryConditions.whyDoomed(mockEntity, mockGame));
        Mockito.reset(mockEntity, mockGame);

        // F1-3 Tornado - Entity is regular infantry (doomed)

        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setWindStrength(PlanetaryConditions.WI_TORNADO_F13);
        mockEntity = Mockito.mock(Infantry.class);
        Mockito.when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        assertEquals("tornado", planetaryConditions.whyDoomed(mockEntity, mockGame));
        Mockito.reset(mockEntity, mockGame);

        // F1-3 Tornado - Entity is battle armor infantry (not doomed)

        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setWindStrength(PlanetaryConditions.WI_TORNADO_F13);
        mockEntity = Mockito.mock(BattleArmor.class);
        Mockito.when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        assertNull(planetaryConditions.whyDoomed(mockEntity, mockGame));
        Mockito.reset(mockEntity, mockGame);

        // Storm - Entity is regular infantry (doomed)

        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setWindStrength(PlanetaryConditions.WI_STORM);
        mockEntity = Mockito.mock(Infantry.class);
        Mockito.when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        assertEquals("storm", planetaryConditions.whyDoomed(mockEntity, mockGame));
        Mockito.reset(mockEntity, mockGame);

        // Storm - Entity is battle armor infantry (not doomed)

        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setWindStrength(PlanetaryConditions.WI_STORM);
        mockEntity = Mockito.mock(BattleArmor.class);
        Mockito.when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        assertNull(planetaryConditions.whyDoomed(mockEntity, mockGame));
        Mockito.reset(mockEntity, mockGame);

        // Extreme temperature - Doomed in extreme temperature, but sheltered in building (not doomed)
        // FIXME: This test is really coupled with Compute.isInBuilding() implementation. It would be nice if I
        //  could mock a static class somehow and abstract the whole thing.

        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setTemperature(100);
        Mockito.when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        Mockito.when(mockGame.getBoard()).thenReturn(mockBoard);
        Mockito.when(mockBoard.getHex(Mockito.any())).thenReturn(mockHex);
        Mockito.when(mockHex.containsTerrain(Terrains.BLDG_ELEV)).thenReturn(true);
        Mockito.when(mockHex.containsTerrain(Terrains.BUILDING)).thenReturn(true);
        Mockito.when(mockHex.terrainLevel(Terrains.BLDG_ELEV)).thenReturn(2);
        mockEntity = Mockito.mock(Infantry.class);
        Mockito.when(mockEntity.doomedInExtremeTemp()).thenReturn(true);
        Mockito.when(mockEntity.getPosition()).thenReturn(mockCoords);
        Mockito.when(mockEntity.getElevation()).thenReturn(1);
        assertNull(planetaryConditions.whyDoomed(mockEntity, mockGame));
        Mockito.reset(mockEntity, mockGame, mockBoard, mockHex);

        // Extreme temperature - Doomed in extreme temperature (doomed)

        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setTemperature(100);
        Mockito.when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
        mockEntity = Mockito.mock(Infantry.class);
        Mockito.when(mockEntity.doomedInExtremeTemp()).thenReturn(true);
        assertEquals("extreme temperature", planetaryConditions.whyDoomed(mockEntity, mockGame));
        Mockito.reset(mockEntity, mockGame);
    }

    @Test
    public void testIsExtremeTemperature() {
        PlanetaryConditions planetaryConditions;

        // Extreme temperature - Heat

        planetaryConditions = new PlanetaryConditions();
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

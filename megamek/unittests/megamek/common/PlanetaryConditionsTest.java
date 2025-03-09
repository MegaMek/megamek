/*
 * Copyright (c) 2024-2025 - The MegaMek Team. All Rights Reserved.
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

import megamek.common.planetaryconditions.Atmosphere;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.common.planetaryconditions.Wind;
import megamek.common.planetaryconditions.WindDirection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    /**
     * @see PlanetaryConditions#determineWind()
     */
    @Nested
    class DetermineWindTests {
        PlanetaryConditions planetaryConditions;

        @BeforeEach
        void beforeEach() {
            planetaryConditions = new PlanetaryConditions();
            planetaryConditions.setWindDirection(WindDirection.NORTH); //Set an initial direction
            planetaryConditions.setWind(Wind.MOD_GALE);
        }

        @Test
        void testNoOptionsEnabled() {
            // Arrange
            try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {

                // Act
                planetaryConditions.determineWind();

                // Assert
                compute.verify(Compute::d6, times(0));
            }
        }

        @Nested
        class ThinOrTraceAtmosphereTests {

            private static Stream<Arguments> atmosphereAndStrongestWind() {
                return Stream.of(
                      Arguments.of(Atmosphere.TRACE, Wind.STORM),
                      Arguments.of(Atmosphere.THIN, Wind.TORNADO_F1_TO_F3)
                );
            }

            @ParameterizedTest
            @MethodSource(value = "atmosphereAndStrongestWind")
            void testStrongestWindValid(Atmosphere atmosphere, Wind strongestWind) {
                // Arrange
                planetaryConditions.setWind(strongestWind);
                planetaryConditions.setAtmosphere(atmosphere);
                try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {

                    // Act
                    planetaryConditions.determineWind();

                    // Assert
                    compute.verify(Compute::d6, times(0));
                    // Nothing should've changed
                    assertEquals(strongestWind, planetaryConditions.getWind());
                }
            }

            @ParameterizedTest
            @MethodSource(value = "atmosphereAndStrongestWind")
            void testStrongerWindSlowed(Atmosphere atmosphere, Wind strongestWind) {
                // Arrange
                // We need to increase the wind beyond the "strongest" wind to verify it gets slowed.
                planetaryConditions.setWind(strongestWind.raiseWind());
                planetaryConditions.setAtmosphere(atmosphere);
                try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {

                    // Act
                    planetaryConditions.determineWind();

                    // Assert
                    compute.verify(Compute::d6, times(0));
                    assertEquals(strongestWind, planetaryConditions.getWind());
                }
            }
        }

        @Nested
        class ShiftingWindDirectionTests {
            @BeforeEach
            void beforeEach() {
                planetaryConditions.setShiftingWindDirection(true);
            }

            @Test
            void testShiftingWindDirectionEnabled () {
                // Arrange
                try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {

                    // Act
                    planetaryConditions.determineWind();

                    // Assert
                    // Should be rolled once
                    compute.verify(Compute::d6, times(1));
                }
            }

            @Test
            void testShiftingWindDirectionCW () {
                // Arrange
                try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
                    compute.when(Compute::d6).thenReturn(1);

                    // Act
                    planetaryConditions.determineWind();

                    // Assert
                    // Should be rolled once
                    compute.verify(Compute::d6, times(1));
                    assertEquals(WindDirection.NORTHEAST, planetaryConditions.getWindDirection());
                }
            }
            @Test
            void testShiftingWindDirectionCCW () {
                // Arrange
                try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
                    compute.when(Compute::d6).thenReturn(6);

                    // Act
                    planetaryConditions.determineWind();

                    // Assert
                    // Should be rolled once
                    compute.verify(Compute::d6, times(1));
                    assertEquals(WindDirection.NORTHWEST, planetaryConditions.getWindDirection());
                }
            }
        }

        @Nested
        class ShiftingWindStrengthTests {
            @BeforeEach
            void beforeEach() {
                planetaryConditions.setShiftingWindStrength(true);
            }

            @Test
            void testShiftingWindStrengthEnabled () {
                // Arrange
                try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {

                    // Act
                    planetaryConditions.determineWind();

                    // Assert
                    // Should be rolled once
                    compute.verify(Compute::d6, times(1));
                }
            }

            @Test
            void testShiftingWindStrengthDecreaseStrength () {
                // Arrange
                try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
                    compute.when(Compute::d6).thenReturn(1);

                    // Act
                    planetaryConditions.determineWind();

                    // Assert
                    // Should be rolled once
                    compute.verify(Compute::d6, times(1));
                    assertEquals(Wind.LIGHT_GALE, planetaryConditions.getWind());
                }
            }

            @Test
            void testShiftingWindStrengthDecreaseTowardsLimit () {
                // Arrange
                planetaryConditions.setWindMin(Wind.LIGHT_GALE);
                try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
                    compute.when(Compute::d6).thenReturn(1);

                    // Act
                    planetaryConditions.determineWind();

                    // Assert
                    // Should be rolled once
                    compute.verify(Compute::d6, times(1));
                    assertEquals(Wind.LIGHT_GALE, planetaryConditions.getWind());
                }
            }

            @Test
            void testShiftingWindStrengthDecreasePastLimit () {
                // Arrange
                planetaryConditions.setWindMin(Wind.LIGHT_GALE);
                try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
                    compute.when(Compute::d6).thenReturn(1);

                    // Act
                    planetaryConditions.determineWind();
                    // Run it again - It should not change the speed
                    planetaryConditions.determineWind();

                    // Assert
                    // Should have rolled twice, but still only a light gale
                    compute.verify(Compute::d6, times(2));
                    assertEquals(Wind.LIGHT_GALE, planetaryConditions.getWind());
                }
            }

            @Test
            void testShiftingWindStrengthIncreaseStrength () {
                // Arrange
                try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
                    compute.when(Compute::d6).thenReturn(6);

                    // Act
                    planetaryConditions.determineWind();

                    // Assert
                    // Should be rolled once
                    compute.verify(Compute::d6, times(1));
                    assertEquals(Wind.STORM, planetaryConditions.getWind());
                }
            }

            @Test
            void testShiftingWindStrengthIncreaseTowardsLimit () {
                // Arrange
                planetaryConditions.setWindMax(Wind.STORM);
                try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
                    compute.when(Compute::d6).thenReturn(6);

                    // Act
                    planetaryConditions.determineWind();

                    // Assert
                    // Should be rolled once
                    compute.verify(Compute::d6, times(1));
                    assertEquals(Wind.STORM, planetaryConditions.getWind());
                }
            }

            @Test
            void testShiftingWindStrengthIncreasePastLimit () {
                // Arrange
                planetaryConditions.setWindMax(Wind.STORM);
                try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
                    compute.when(Compute::d6).thenReturn(6);

                    // Act
                    planetaryConditions.determineWind();
                    // Run it again - It should not change the speed
                    planetaryConditions.determineWind();

                    // Assert
                    // Should be rolled twice, but still just a storm
                    compute.verify(Compute::d6, times(2));
                    assertEquals(Wind.STORM, planetaryConditions.getWind());
                }
            }
        }
    }
}

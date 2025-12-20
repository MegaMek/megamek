/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.stream.Stream;

import megamek.common.board.Board;
import megamek.common.board.BoardType;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.Tank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ComputeVisualRangeTest {

    /**
     * Testing under normal planetary conditions.
     */
    @Test
    void testVisualRange() {
        // Mock Player
        Player mockPlayer = mock(Player.class);

        // Mock the board
        Board mockBoard = mock(Board.class);
        when(mockBoard.isSpace()).thenReturn(false);

        // Mock Options
        GameOptions mockOptions = mock(GameOptions.class);
        when(mockOptions.booleanOption(anyString())).thenReturn(false);

        // Mock Planetary Conditions
        PlanetaryConditions mockPlanetaryConditions = new PlanetaryConditions();


        Game mockGame = mock(Game.class);
        when(mockGame.getOptions()).thenReturn(mockOptions);


        LosEffects mockLos = mock(LosEffects.class);
        when(mockGame.getBoard()).thenReturn(mockBoard);
        when(mockGame.getPlayer(anyInt())).thenReturn(mockPlayer);
        when(mockGame.getFlares()).thenReturn(new Vector<>());
        when(mockGame.getPlanetaryConditions()).thenReturn(mockPlanetaryConditions);


        Tank mockAttackingEntity = mock(Tank.class);
        when(mockAttackingEntity.getPosition()).thenReturn(new Coords(0, 0));

        Tank mockTarget = mock(Tank.class);
        when(mockTarget.getPosition()).thenReturn(new Coords(0, 7));
        when(mockTarget.isIlluminated()).thenReturn(true);

        assertTrue(Compute.inVisualRange(mockGame, mockLos, mockAttackingEntity, mockTarget));

        // Move our target too far away
        when(mockTarget.getPosition()).thenReturn(new Coords(0, 61));
        assertFalse(Compute.inVisualRange(mockGame, mockLos, mockAttackingEntity, mockTarget));

        // Move our target just close enough
        when(mockTarget.getPosition()).thenReturn(new Coords(0, 60));
        assertTrue(Compute.inVisualRange(mockGame, mockLos, mockAttackingEntity, mockTarget));
    }


    /**
     * Testing under normal planetary conditions, air to ground
     */
    @Test
    void testVisualRangeAirToGround() {
        // Mock Player
        Player mockPlayer = mock(Player.class);

        // Mock the board
        Board mockBoard = mock(Board.class);
        when(mockBoard.isSpace()).thenReturn(false);

        // Mock Options
        GameOptions mockOptions = mock(GameOptions.class);
        when(mockOptions.booleanOption(anyString())).thenReturn(false);

        // Mock Planetary Conditions
        PlanetaryConditions mockPlanetaryConditions = new PlanetaryConditions();


        Game mockGame = mock(Game.class);
        when(mockGame.getOptions()).thenReturn(mockOptions);


        LosEffects mockLos = mock(LosEffects.class);
        when(mockGame.getBoard()).thenReturn(mockBoard);
        when(mockGame.getPlayer(anyInt())).thenReturn(mockPlayer);
        when(mockGame.getFlares()).thenReturn(new Vector<>());
        when(mockGame.getPlanetaryConditions()).thenReturn(mockPlanetaryConditions);


        Entity mockAttackingEntity = mock(Aero.class);
        when(mockAttackingEntity.getPosition()).thenReturn(new Coords(0, 0));
        when(mockAttackingEntity.getAltitude()).thenReturn(5);
        when(mockAttackingEntity.isAirborne()).thenReturn(true);
        when(mockAttackingEntity.isAero()).thenReturn(true);
        when(mockAttackingEntity.isAirborneAeroOnGroundMap()).thenReturn(true);
        Vector<Coords> position = new Vector<>(List.of(mockAttackingEntity.getPosition()));
        when(mockAttackingEntity.getPassedThrough()).thenReturn(position);

        Entity mockTarget = mock(Tank.class);
        when(mockTarget.getPosition()).thenReturn(new Coords(0, 7));
        when(mockTarget.isIlluminated()).thenReturn(true);
        when(mockTarget.isAirborne()).thenReturn(false);
        when(mockTarget.isAirborneAeroOnGroundMap()).thenReturn(false);

        assertTrue(Compute.inVisualRange(mockGame, mockLos, mockAttackingEntity, mockTarget));

        // Move our target too far away
        when(mockTarget.getPosition()).thenReturn(new Coords(0, 120));
        assertFalse(Compute.inVisualRange(mockGame, mockLos, mockAttackingEntity, mockTarget));

        // Move our target just close enough
        when(mockTarget.getPosition()).thenReturn(new Coords(0, 110));
        assertTrue(Compute.inVisualRange(mockGame, mockLos, mockAttackingEntity, mockTarget));

        // Let's change our altitude - if we go altitude 10 we can't see any ground units
        when(mockAttackingEntity.getAltitude()).thenReturn(10);
        when(mockTarget.getPosition()).thenReturn(new Coords(0, 10));
        assertFalse(Compute.inVisualRange(mockGame, mockLos, mockAttackingEntity, mockTarget));

        // Let's get low - if we go to NOE we should use "ground" distance
        when(mockAttackingEntity.getAltitude()).thenReturn(1);
        when(mockTarget.getPosition()).thenReturn(new Coords(0, 90));
        assertFalse(Compute.inVisualRange(mockGame, mockLos, mockAttackingEntity, mockTarget));

        // But if we bring the enemy a little closer, we'll be able to see them
        when(mockTarget.getPosition()).thenReturn(new Coords(0, 58));
        assertTrue(Compute.inVisualRange(mockGame, mockLos, mockAttackingEntity, mockTarget));
    }


    /**
     * Testing under normal planetary conditions, ground to air
     */
    @Test
    void testVisualRangeGroundToAir() {
        // Mock Player
        Player mockPlayer = mock(Player.class);

        // Mock the board
        Board mockBoard = mock(Board.class);
        when(mockBoard.isSpace()).thenReturn(false);

        // Mock Options
        GameOptions mockOptions = mock(GameOptions.class);
        when(mockOptions.booleanOption(anyString())).thenReturn(false);

        // Mock Planetary Conditions
        PlanetaryConditions mockPlanetaryConditions = new PlanetaryConditions();


        Game mockGame = mock(Game.class);
        when(mockGame.getOptions()).thenReturn(mockOptions);


        LosEffects mockLos = mock(LosEffects.class);
        when(mockGame.getBoard()).thenReturn(mockBoard);
        when(mockGame.getPlayer(anyInt())).thenReturn(mockPlayer);
        when(mockGame.getFlares()).thenReturn(new Vector<>());
        when(mockGame.getPlanetaryConditions()).thenReturn(mockPlanetaryConditions);


        Tank mockAttackingEntity = mock(Tank.class);
        when(mockAttackingEntity.getPosition()).thenReturn(new Coords(0, 0));
        when(mockAttackingEntity.isAirborne()).thenReturn(false);
        when(mockAttackingEntity.isAirborneAeroOnGroundMap()).thenReturn(false);


        Aero mockTarget = mock(Aero.class);
        when(mockTarget.getAltitude()).thenReturn(5);
        when(mockTarget.getPosition()).thenReturn(new Coords(0, 7));
        when(mockTarget.isIlluminated()).thenReturn(true);
        when(mockTarget.isAirborne()).thenReturn(true);
        when(mockTarget.isAirborneAeroOnGroundMap()).thenReturn(true);
        when(mockTarget.isAero()).thenReturn(true);
        when(mockTarget.getPassedThrough()).thenReturn(new Vector<>(Collections.singleton(new Coords(0, 7))));

        assertTrue(Compute.inVisualRange(mockGame, mockLos, mockAttackingEntity, mockTarget));

        // Move our target too far away
        when(mockTarget.getPosition()).thenReturn(new Coords(0, 60));
        when(mockTarget.getPassedThrough()).thenReturn(new Vector<>(Collections.singleton(new Coords(0, 60))));
        assertFalse(Compute.inVisualRange(mockGame, mockLos, mockAttackingEntity, mockTarget));

        // Move our target just close enough
        when(mockTarget.getPosition()).thenReturn(new Coords(0, 50));
        when(mockTarget.getPassedThrough()).thenReturn(new Vector<>(Collections.singleton(new Coords(0, 50))));
        assertTrue(Compute.inVisualRange(mockGame, mockLos, mockAttackingEntity, mockTarget));

    }

    /**
     * Nested test class for visual range testing at different altitudes on a low altitude board.
     * Tests altitudes 1 through 10 to verify visibility of targets at different elevations.
     */
    @Nested
    public class LowAltitudeVisibilityTests {

        private Game mockGame;
        private Board mockBoard;
        private LosEffects mockLos;
        private GameOptions mockOptions;
        private PlanetaryConditions mockPlanetaryConditions;
        private Player mockPlayer;

        @BeforeEach
        void setUp() {
            // Mock Player
            mockPlayer = mock(Player.class);

            // Mock the board - LOW_ALTITUDE board (SKY type)
            mockBoard = mock(Board.class);
            when(mockBoard.isSpace()).thenReturn(false);
            when(mockBoard.isLowAltitude()).thenReturn(true);

            // Mock Options
            mockOptions = mock(GameOptions.class);
            when(mockOptions.booleanOption(anyString())).thenReturn(false);

            // Mock Planetary Conditions
            mockPlanetaryConditions = new PlanetaryConditions();

            // Mock Game
            mockGame = mock(Game.class);
            when(mockGame.getOptions()).thenReturn(mockOptions);
            when(mockGame.getBoard()).thenReturn(mockBoard);
            when(mockGame.getPlayer(anyInt())).thenReturn(mockPlayer);
            when(mockGame.getFlares()).thenReturn(new Vector<>());
            when(mockGame.getPlanetaryConditions()).thenReturn(mockPlanetaryConditions);

            // Mock LOS
            mockLos = mock(LosEffects.class);
        }

        /**
         * Provides altitudes 1 through 10 for parameterized tests.
         */
        private static Stream<Integer> getAltitudes() {
            return Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        }

        /**
         * Tests air-to-air visibility at different altitudes on a low altitude board.
         * Verifies that an airborne entity at a specific altitude can see other airborne targets at different altitudes.
         *
         * @param altitude the altitude of the attacking entity (1-10)
         */
        @ParameterizedTest
        @MethodSource("getAltitudes")
        void testVisibilityAtAltitude(int altitude) {
            // Arrange - Create attacking entity at specified altitude
            Aero mockAttackingEntity = mock(Aero.class);
            when(mockAttackingEntity.getPosition()).thenReturn(new Coords(0, 0));
            when(mockAttackingEntity.getAltitude()).thenReturn(altitude);
            when(mockAttackingEntity.isAirborne()).thenReturn(true);
            when(mockAttackingEntity.isAero()).thenReturn(true);
            // Board is low altitude, so this is not on a ground map.
            when(mockAttackingEntity.isAirborneAeroOnGroundMap()).thenReturn(false);

            // Create mock airborne target at close range
            Aero mockTarget = mock(Aero.class);
            when(mockTarget.getPosition()).thenReturn(new Coords(0, 10));
            when(mockTarget.isIlluminated()).thenReturn(true);
            when(mockTarget.isAirborne()).thenReturn(true);
            when(mockTarget.isAero()).thenReturn(true);
            // Board is low altitude, so this is not on a ground map.
            when(mockTarget.isAirborneAeroOnGroundMap()).thenReturn(false);
            when(mockTarget.getPassedThrough()).thenReturn(new Vector<>(Collections.singleton(new Coords(0, 10))));

            // Test at different altitudes for the target
            for (int targetAltitude = 1; targetAltitude <= 10; targetAltitude++) {
                when(mockTarget.getAltitude()).thenReturn(targetAltitude);
                when(mockTarget.getPassedThrough()).thenReturn(new Vector<>(Collections.singleton(new Coords(0, 10))));

                // Act & Assert - Close range should be visible
                assertTrue(Compute.inVisualRange(mockGame, mockLos, mockAttackingEntity, mockTarget),
                    String.format("Entity at altitude %d should see airborne target at altitude %d, range 10",
                        altitude, targetAltitude));
            }
        }

        /**
         * Tests that airborne targets beyond visual range are not visible.
         *
         * @param altitude the altitude of the attacking entity (1-10)
         */
        @ParameterizedTest
        @MethodSource("getAltitudes")
        void testBeyondVisualRangeAtAltitude(int altitude) {
            // Arrange
            Aero mockAttackingEntity = mock(Aero.class);
            when(mockAttackingEntity.getPosition()).thenReturn(new Coords(0, 0));
            when(mockAttackingEntity.getAltitude()).thenReturn(altitude);
            when(mockAttackingEntity.isAirborne()).thenReturn(true);
            when(mockAttackingEntity.isAero()).thenReturn(true);
            // Board is low altitude, so this is not on a ground map.
            when(mockAttackingEntity.isAirborneAeroOnGroundMap()).thenReturn(false);

            Aero mockTarget = mock(Aero.class);
            when(mockTarget.getPosition()).thenReturn(new Coords(0, 150)); // Very far away
            when(mockTarget.getAltitude()).thenReturn(altitude);
            when(mockTarget.isIlluminated()).thenReturn(true);
            when(mockTarget.isAirborne()).thenReturn(true);
            when(mockTarget.isAero()).thenReturn(true);
            // Board is low altitude, so this is not on a ground map.
            when(mockTarget.isAirborneAeroOnGroundMap()).thenReturn(false);
            when(mockTarget.getPassedThrough()).thenReturn(new Vector<>(Collections.singleton(new Coords(0, 150))));

            // Act & Assert - Target should be out of visual range
            assertFalse(Compute.inVisualRange(mockGame, mockLos, mockAttackingEntity, mockTarget),
                String.format("Entity at altitude %d should NOT see airborne target at range 150", altitude));
        }

        /**
         * Tests visibility of airborne targets at the same altitude.
         *
         * @param altitude the altitude of both entities (1-10)
         */
        @ParameterizedTest
        @MethodSource("getAltitudes")
        void testAirToAirAtSameAltitude(int altitude) {
            // Arrange - Both entities at same altitude
            Aero mockAttackingEntity = mock(Aero.class);
            when(mockAttackingEntity.getPosition()).thenReturn(new Coords(0, 0));
            when(mockAttackingEntity.getAltitude()).thenReturn(altitude);
            when(mockAttackingEntity.isAirborne()).thenReturn(true);
            when(mockAttackingEntity.isAero()).thenReturn(true);
            // Board is low altitude, so this is not on a ground map.
            when(mockAttackingEntity.isAirborneAeroOnGroundMap()).thenReturn(false);

            Aero mockTarget = mock(Aero.class);
            when(mockTarget.getPosition()).thenReturn(new Coords(0, 20));
            when(mockTarget.getAltitude()).thenReturn(altitude);
            when(mockTarget.isIlluminated()).thenReturn(true);
            when(mockTarget.isAirborne()).thenReturn(true);
            when(mockTarget.isAero()).thenReturn(true);
            // Board is low altitude, so this is not on a ground map.
            when(mockTarget.isAirborneAeroOnGroundMap()).thenReturn(false);
            when(mockTarget.getPassedThrough()).thenReturn(new Vector<>(Collections.singleton(new Coords(0, 20))));

            // Act & Assert - Should see target at same altitude
            assertTrue(Compute.inVisualRange(mockGame, mockLos, mockAttackingEntity, mockTarget),
                String.format("Entities at same altitude %d should see each other at range 20", altitude));
        }
    }

    /**
     * Nested test class for air-to-air visual range testing on ground maps.
     * Tests altitudes 1 through 10 to verify visibility of airborne targets at 30 hexes distance.
     */
    @Nested
    public class AirToAirOnGroundMapTests {

        private Game mockGame;
        private Board mockBoard;
        private LosEffects mockLos;
        private GameOptions mockOptions;
        private PlanetaryConditions mockPlanetaryConditions;
        private Player mockPlayer;

        @BeforeEach
        void setUp() {
            // Mock Player
            mockPlayer = mock(Player.class);

            // Mock the board - GROUND map
            mockBoard = mock(Board.class);
            when(mockBoard.isSpace()).thenReturn(false);
            when(mockBoard.isLowAltitude()).thenReturn(false);
            when(mockBoard.isGround()).thenReturn(true);

            // Mock Options
            mockOptions = mock(GameOptions.class);
            when(mockOptions.booleanOption(anyString())).thenReturn(false);

            // Mock Planetary Conditions
            mockPlanetaryConditions = new PlanetaryConditions();

            // Mock Game
            mockGame = mock(Game.class);
            when(mockGame.getOptions()).thenReturn(mockOptions);
            when(mockGame.getBoard()).thenReturn(mockBoard);
            when(mockGame.getPlayer(anyInt())).thenReturn(mockPlayer);
            when(mockGame.getFlares()).thenReturn(new Vector<>());
            when(mockGame.getPlanetaryConditions()).thenReturn(mockPlanetaryConditions);

            // Mock LOS
            mockLos = mock(LosEffects.class);
        }

        /**
         * Provides altitudes 1 through 10 for parameterized tests.
         */
        private static Stream<Integer> getAltitudes() {
            return Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        }

        /**
         * Tests that an airborne attacker at various altitudes can see another aircraft 30 hexes away.
         * Verifies air-to-air visibility on ground maps.
         *
         * @param altitude the altitude of the attacking entity (1-10)
         */
        @ParameterizedTest
        @MethodSource("getAltitudes")
        void testAirToAirVisibilityAt30Hexes(int altitude) {
            // Arrange - Create attacking entity at specified altitude
            Aero mockAttackingEntity = mock(Aero.class);
            when(mockAttackingEntity.getPosition()).thenReturn(new Coords(0, 0));
            when(mockAttackingEntity.getAltitude()).thenReturn(altitude);
            when(mockAttackingEntity.isAirborne()).thenReturn(true);
            when(mockAttackingEntity.isAero()).thenReturn(true);
            when(mockAttackingEntity.isAirborneAeroOnGroundMap()).thenReturn(true);

            // Create mock airborne target 30 hexes away at the same altitude
            Aero mockTarget = mock(Aero.class);
            when(mockTarget.getPosition()).thenReturn(new Coords(0, 30));
            when(mockTarget.getAltitude()).thenReturn(altitude);
            when(mockTarget.isIlluminated()).thenReturn(true);
            when(mockTarget.isAirborne()).thenReturn(true);
            when(mockTarget.isAero()).thenReturn(true);
            when(mockTarget.isAirborneAeroOnGroundMap()).thenReturn(true);
            when(mockTarget.getPassedThrough()).thenReturn(new Vector<>(Collections.singleton(new Coords(0, 30))));

            // Act & Assert - Attacker should see target at 30 hexes
            assertTrue(Compute.inVisualRange(mockGame, mockLos, mockAttackingEntity, mockTarget),
                String.format("Aero at altitude %d should see another aero at 30 hexes on ground map", altitude));
        }

        /**
         * Tests air-to-air visibility at different target altitudes.
         *
         * @param attackerAltitude the altitude of the attacking entity (1-10)
         */
        @ParameterizedTest
        @MethodSource("getAltitudes")
        void testAirToAirAtDifferentAltitudes(int attackerAltitude) {
            // Arrange - Create attacking entity at specified altitude
            Aero mockAttackingEntity = mock(Aero.class);
            when(mockAttackingEntity.getPosition()).thenReturn(new Coords(0, 0));
            when(mockAttackingEntity.getAltitude()).thenReturn(attackerAltitude);
            when(mockAttackingEntity.isAirborne()).thenReturn(true);
            when(mockAttackingEntity.isAero()).thenReturn(true);
            when(mockAttackingEntity.isAirborneAeroOnGroundMap()).thenReturn(true);

            // Test targets at various altitudes
            Aero mockTarget = mock(Aero.class);
            when(mockTarget.getPosition()).thenReturn(new Coords(0, 30));
            when(mockTarget.isIlluminated()).thenReturn(true);
            when(mockTarget.isAirborne()).thenReturn(true);
            when(mockTarget.isAero()).thenReturn(true);
            when(mockTarget.isAirborneAeroOnGroundMap()).thenReturn(true);
            when(mockTarget.getPassedThrough()).thenReturn(new Vector<>(Collections.singleton(new Coords(0, 30))));

            // Test at different target altitudes
            for (int targetAltitude = 1; targetAltitude <= 10; targetAltitude++) {
                when(mockTarget.getAltitude()).thenReturn(targetAltitude);

                // Act & Assert - Should see target at different altitudes
                assertTrue(Compute.inVisualRange(mockGame, mockLos, mockAttackingEntity, mockTarget),
                    String.format("Aero at altitude %d should see aero at altitude %d, 30 hexes away",
                        attackerAltitude, targetAltitude));
            }
        }
    }
}

/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.bot.princess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.units.EjectedCrew;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for InfantryCombatHelper class.
 */
class InfantryCombatHelperTest {

    /**
     * Tests for {@link InfantryCombatHelper#getEnemyInfantryInHex(Game, Coords, int)}.
     */
    @Nested
    class GetEnemyInfantryInHexTests {

        @Test
        void testReturnsInfantryInBuilding_ExcludesInfantryOutside() {
            // Arrange
            Game mockGame = mock(Game.class);
            Coords coords = new Coords(5, 5);
            int ownerId = 1;

            Infantry infantryInBuilding = mock(Infantry.class);
            when(infantryInBuilding.getOwnerId()).thenReturn(2);
            when(infantryInBuilding.isInBuilding()).thenReturn(true);

            Infantry infantryOutside = mock(Infantry.class);
            when(infantryOutside.getOwnerId()).thenReturn(2);
            when(infantryOutside.isInBuilding()).thenReturn(false);

            List<Entity> entitiesAtCoords = new ArrayList<>();
            entitiesAtCoords.add(infantryInBuilding);
            entitiesAtCoords.add(infantryOutside);
            when(mockGame.getEntitiesVector(coords)).thenReturn(entitiesAtCoords);

            // Act
            List<Entity> result = InfantryCombatHelper.getEnemyInfantryInHex(
                  mockGame, coords, ownerId);

            // Assert
            assertEquals(1, result.size());
            assertTrue(result.contains(infantryInBuilding));
        }

        @Test
        void testExcludesEjectedCrew() {
            // Arrange
            Game mockGame = mock(Game.class);
            Coords coords = new Coords(5, 5);
            int ownerId = 1;

            EjectedCrew ejectedCrew = mock(EjectedCrew.class);
            when(ejectedCrew.getOwnerId()).thenReturn(2);
            when(ejectedCrew.isInBuilding()).thenReturn(true);

            Infantry infantry = mock(Infantry.class);
            when(infantry.getOwnerId()).thenReturn(2);
            when(infantry.isInBuilding()).thenReturn(true);

            List<Entity> entitiesAtCoords = new ArrayList<>();
            entitiesAtCoords.add(ejectedCrew);
            entitiesAtCoords.add(infantry);
            when(mockGame.getEntitiesVector(coords)).thenReturn(entitiesAtCoords);

            // Act
            List<Entity> result = InfantryCombatHelper.getEnemyInfantryInHex(
                  mockGame, coords, ownerId);

            // Assert
            assertEquals(1, result.size());
            assertTrue(result.contains(infantry));
        }

        @Test
        void testExcludesFriendlyInfantry() {
            // Arrange
            Game mockGame = mock(Game.class);
            Coords coords = new Coords(5, 5);
            int ownerId = 1;

            Infantry friendlyInfantry = mock(Infantry.class);
            when(friendlyInfantry.getOwnerId()).thenReturn(1);
            when(friendlyInfantry.isInBuilding()).thenReturn(true);

            Infantry enemyInfantry = mock(Infantry.class);
            when(enemyInfantry.getOwnerId()).thenReturn(2);
            when(enemyInfantry.isInBuilding()).thenReturn(true);

            List<Entity> entitiesAtCoords = new ArrayList<>();
            entitiesAtCoords.add(friendlyInfantry);
            entitiesAtCoords.add(enemyInfantry);
            when(mockGame.getEntitiesVector(coords)).thenReturn(entitiesAtCoords);

            // Act
            List<Entity> result = InfantryCombatHelper.getEnemyInfantryInHex(
                  mockGame, coords, ownerId);

            // Assert
            assertEquals(1, result.size());
            assertTrue(result.contains(enemyInfantry));
        }

        @Test
        void testReturnsEmpty_WhenNoInfantryInBuilding() {
            // Arrange
            Game mockGame = mock(Game.class);
            Coords coords = new Coords(5, 5);
            int ownerId = 1;

            Infantry infantry1 = mock(Infantry.class);
            when(infantry1.getOwnerId()).thenReturn(2);
            when(infantry1.isInBuilding()).thenReturn(false);

            Infantry infantry2 = mock(Infantry.class);
            when(infantry2.getOwnerId()).thenReturn(2);
            when(infantry2.isInBuilding()).thenReturn(false);

            List<Entity> entitiesAtCoords = new ArrayList<>();
            entitiesAtCoords.add(infantry1);
            entitiesAtCoords.add(infantry2);
            when(mockGame.getEntitiesVector(coords)).thenReturn(entitiesAtCoords);

            // Act
            List<Entity> result = InfantryCombatHelper.getEnemyInfantryInHex(
                  mockGame, coords, ownerId);

            // Assert
            assertEquals(0, result.size());
        }

        @Test
        void testReturnsAllEnemyInfantry_WhenMultipleInBuilding() {
            // Arrange
            Game mockGame = mock(Game.class);
            Coords coords = new Coords(5, 5);
            int ownerId = 1;

            Infantry infantry1 = mock(Infantry.class);
            when(infantry1.getOwnerId()).thenReturn(2);
            when(infantry1.isInBuilding()).thenReturn(true);

            Infantry infantry2 = mock(Infantry.class);
            when(infantry2.getOwnerId()).thenReturn(3);
            when(infantry2.isInBuilding()).thenReturn(true);

            Infantry infantry3 = mock(Infantry.class);
            when(infantry3.getOwnerId()).thenReturn(2);
            when(infantry3.isInBuilding()).thenReturn(true);

            List<Entity> entitiesAtCoords = new ArrayList<>();
            entitiesAtCoords.add(infantry1);
            entitiesAtCoords.add(infantry2);
            entitiesAtCoords.add(infantry3);
            when(mockGame.getEntitiesVector(coords)).thenReturn(entitiesAtCoords);

            // Act
            List<Entity> result = InfantryCombatHelper.getEnemyInfantryInHex(
                  mockGame, coords, ownerId);

            // Assert
            assertEquals(3, result.size());
            assertTrue(result.contains(infantry1));
            assertTrue(result.contains(infantry2));
            assertTrue(result.contains(infantry3));
        }
    }
}

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
package megamek.client.bot.princess;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import megamek.common.BipedMek;
import megamek.common.Coords;
import megamek.common.Entity;
import org.junit.jupiter.api.Test;

/**
 * @author Luana Coppio
 */
class UnitsMedianCoordinateCalculatorTest {

    @Test
    void testNoEnemies() {
        List<Entity> enemies = new ArrayList<>();
        Coords myPosition = new Coords(10, 10);
        var unitsMedianCalculator = new UnitsMedianCoordinateCalculator(5);
        Coords result = unitsMedianCalculator.getEnemiesMedianCoordinate(enemies, myPosition);

        // When no enemies are present, the function should return null
        assertNull(result);
    }


    @Test
    void testOneEnemyMoved() {
        // One enemy that has moved to position (5, 5)
        List<Entity> enemies = List.of(enemyMoved(new Coords(5, 5)));
        Coords myPosition = new Coords(10, 10);
        var unitsMedianCalculator = new UnitsMedianCoordinateCalculator(5);
        Coords result = unitsMedianCalculator.getEnemiesMedianCoordinate(enemies, myPosition);

        // With only one enemy, the median should be that enemy's position
        assertEquals(new Coords(5, 5), result);
    }

    @Test
    void testThreeEnemiesTwoMoved() {
        // Creating three enemies with different positions
        // Two have "moved" to new positions, one is at original position
        List<Entity> enemies = List.of(
              enemyMoved(new Coords(8, 8)),   // Moved enemy 1 - closest
              enemyMoved(new Coords(5, 5)),   // Moved enemy 2 - second closest
              enemy(new Coords(15, 15))  // Unmoved enemy - furthest
        );
        Coords myPosition = new Coords(10, 10);

        var unitsMedianCalculator = new UnitsMedianCoordinateCalculator(5);
        Coords result = unitsMedianCalculator.getEnemiesMedianCoordinate(enemies, myPosition);

        // Median of these three positions should be calculated
        // For these coordinates, median should be (8, 8)
        assertEquals(new Coords(8, 8), result);
    }

    @Test
    void testSevenEnemiesFourMoved() {
        // Seven enemies with different positions and movement states
        List<Entity> enemies = List.of(
              enemyMoved(new Coords(9, 9)),    // Moved enemy 1 - very close
              enemyMoved(new Coords(11, 11)),  // Moved enemy 2 - very close
              enemyMoved(new Coords(12, 8)),   // Moved enemy 3 - close
              enemyMoved(new Coords(8, 12)),   // Moved enemy 4 - close
              enemy(new Coords(5, 5)),    // Unmoved enemy 1 - medium distance
              enemy(new Coords(15, 15)),  // Unmoved enemy 2 - far
              enemy(new Coords(20, 20))   // Unmoved enemy 3 - very far
        );
        Coords myPosition = new Coords(10, 10);


        var unitsMedianCalculator = new UnitsMedianCoordinateCalculator(5);
        Coords result = unitsMedianCalculator.getEnemiesMedianCoordinate(enemies, myPosition);

        // The function should consider only the 5 closest enemies
        // The 5 closest are: (9,9), (11,11), (12,8), (8,12), (5,5)
        // Median of these should be (9,9)
        assertEquals(new Coords(9, 9), result);
    }

    @Test
    void testChangingPositionsWithLimit() {
        Coords myPosition = new Coords(10, 10);

        // Initial positions - enemy1 and enemy2 are closest
        Entity enemy1 = enemyMoved(new Coords(11, 11));
        Entity enemy2 = enemyImmobile(new Coords(12, 10));
        Entity enemy3 = enemyImmobile(new Coords(14, 14));

        List<Entity> enemies = List.of(enemy1, enemy2, enemy3);

        var unitsMedianCalculator = new UnitsMedianCoordinateCalculator(2);
        Coords median1 = unitsMedianCalculator.getEnemiesMedianCoordinate(enemies, myPosition);

        // With limit 2, only enemy1 and enemy2 are considered
        // Median of (11,11) and (12,10) is (11.5, 10.5) which rounds to (12, 11)
        assertEquals(new Coords(11, 10), median1);

        myPosition = new Coords(15, 15);
        Coords median2 = unitsMedianCalculator.getEnemiesMedianCoordinate(enemies, myPosition);

        // Now enemy3 and enemy1 are the closest
        // Median of (14,14) and (11,11) is (12, 12)
        assertEquals(new Coords(12, 12), median2);
    }

    private static Entity enemy(Coords coords) {
        return createUnitMock(coords, false, false);
    }

    private static Entity enemyImmobile(Coords coords) {
        return createUnitMock(coords, false, true);
    }

    private static Entity enemyMoved(Coords coords) {
        return createUnitMock(coords, true, false);
    }

    private static Entity createUnitMock(Coords coords, boolean moved, boolean immobile) {
        Entity entity = mock(BipedMek.class);
        when(entity.getPosition()).thenReturn(coords);
        when(entity.isSelectableThisTurn()).thenReturn(!moved);
        when(entity.isImmobile()).thenReturn(immobile);
        when(entity.getWalkMP()).thenReturn(2);
        return entity;
    }

}

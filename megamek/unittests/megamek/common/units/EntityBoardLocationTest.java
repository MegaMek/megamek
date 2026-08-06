/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests the cached {@link Entity#getBoardLocation()} override: it must reuse one instance while the position and board
 * id are unchanged, and rebuild correctly when the entity moves.
 */
class EntityBoardLocationTest {

    @BeforeAll
    static void setUpAll() {
        EquipmentType.initializeTypes();
    }

    @Test
    void testGetBoardLocationCachesWhileStationary() {
        Tank entity = new Tank();
        entity.setPosition(new Coords(3, 4), false);

        BoardLocation first = entity.getBoardLocation();
        assertEquals(new Coords(3, 4), first.coords(), "Location should reflect the current position");
        assertEquals(0, first.boardId(), "Default board id is 0");

        assertSame(first, entity.getBoardLocation(), "BoardLocation should be reused while the position is unchanged");
    }

    @Test
    void testGetBoardLocationRebuildsAfterMove() {
        Tank entity = new Tank();
        entity.setPosition(new Coords(3, 4), false);
        BoardLocation before = entity.getBoardLocation();

        entity.setPosition(new Coords(7, 8), false);
        BoardLocation after = entity.getBoardLocation();

        assertNotSame(before, after, "Cache should rebuild after the entity moves");
        assertEquals(new Coords(7, 8), after.coords(), "Rebuilt location should reflect the new position");
    }

    @Test
    void testGetBoardLocationWithoutPositionIsNoLocation() {
        Tank entity = new Tank();
        entity.setPosition(new Coords(3, 4), false);
        entity.getBoardLocation();

        entity.setPosition(null, false);
        assertSame(BoardLocation.NO_LOCATION, entity.getBoardLocation(),
              "A null position should yield the NO_LOCATION singleton");
        assertSame(BoardLocation.NO_LOCATION, entity.getBoardLocation(),
              "Repeated calls without a position should keep returning the NO_LOCATION singleton");
    }
}

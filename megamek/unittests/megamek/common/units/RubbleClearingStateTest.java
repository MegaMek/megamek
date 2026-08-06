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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests the per-vehicle bulldozer rubble-clearing state machine on {@link Tank}: beginning a clear, banking turns and
 * abandoning/finishing it.
 */
class RubbleClearingStateTest {

    @BeforeAll
    static void initEquipment() {
        EquipmentType.initializeTypes();
    }

    @Test
    @DisplayName("A fresh vehicle is not clearing rubble")
    void freshVehicleNotClearing() {
        Tank tank = new Tank();
        assertFalse(tank.isClearingRubble());
        assertNull(tank.getRubbleClearTarget());
    }

    @Test
    @DisplayName("Beginning a clear records the target and required turns")
    void beginClearingRecordsState() {
        Tank tank = new Tank();
        Coords target = new Coords(3, 4);
        tank.beginClearingRubble(target, 8);

        assertTrue(tank.isClearingRubble());
        assertEquals(target, tank.getRubbleClearTarget());
        assertEquals(8, tank.getRubbleClearTurnsRequired());
        assertEquals(0, tank.getRubbleClearTurnsCompleted());
    }

    @Test
    @DisplayName("Banking turns advances the completed count")
    void bankingTurnsAdvancesCount() {
        Tank tank = new Tank();
        tank.beginClearingRubble(new Coords(1, 1), 4);

        assertEquals(1, tank.bankRubbleClearTurn());
        assertEquals(2, tank.bankRubbleClearTurn());
        assertEquals(2, tank.getRubbleClearTurnsCompleted());
    }

    @Test
    @DisplayName("Cancelling clears all rubble-clearing state")
    void cancelClearsState() {
        Tank tank = new Tank();
        tank.beginClearingRubble(new Coords(2, 2), 16);
        tank.bankRubbleClearTurn();

        tank.cancelClearingRubble();

        assertFalse(tank.isClearingRubble());
        assertNull(tank.getRubbleClearTarget());
        assertEquals(0, tank.getRubbleClearTurnsCompleted());
        assertEquals(0, tank.getRubbleClearTurnsRequired());
    }

    @Test
    @DisplayName("Beginning a new clear resets the completed counter")
    void beginResetsCounter() {
        Tank tank = new Tank();
        tank.beginClearingRubble(new Coords(0, 0), 8);
        tank.bankRubbleClearTurn();
        tank.bankRubbleClearTurn();

        tank.beginClearingRubble(new Coords(5, 5), 2);

        assertEquals(0, tank.getRubbleClearTurnsCompleted());
        assertEquals(2, tank.getRubbleClearTurnsRequired());
        assertEquals(new Coords(5, 5), tank.getRubbleClearTarget());
    }
}

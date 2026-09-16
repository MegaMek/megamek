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

package megamek.ai.dataset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import megamek.common.GameBoardTestCase;
import megamek.common.Player;
import megamek.common.enums.MoveStepType;
import megamek.common.moves.MovePath;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Tank;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for megamek #8972: building a dataset row from a finished move must not fail when the move left
 * the unit without a position (it mounted a DropShip, was recovered by a carrier, or left the board).
 */
class UnitActionFromMovePathTest extends GameBoardTestCase {

    static {
        initializeBoard("OPEN_3X3", """
              size 3 3
              hex 0101 0 "" ""
              hex 0201 0 "" ""
              hex 0301 0 "" ""
              hex 0102 0 "" ""
              hex 0202 0 "" ""
              hex 0302 0 "" ""
              hex 0103 0 "" ""
              hex 0203 0 "" ""
              hex 0303 0 "" ""
              end""");
    }

    private MovePath createTankPath() {
        setBoard("OPEN_3X3");
        Player player = new Player(0, "Test Player");
        getGame().addPlayer(0, player);
        Tank tank = new Tank();
        tank.setOwner(player);
        return getMovePathFor(tank, EntityMovementMode.TRACKED, MoveStepType.FORWARDS);
    }

    @Test
    void fromMovePathWithLoadedUnitDoesNotThrow() {
        MovePath movePath = createTankPath();
        // The server loads the unit during processMovement, which clears its position before the path is logged
        movePath.getEntity().setPosition(null);

        UnitAction unitAction = assertDoesNotThrow(() -> UnitAction.fromMovePath(movePath));

        assertEquals(1.0, unitAction.get(UnitAction.Field.CHANCE_OF_FAILURE));
    }

    @Test
    void fromMovePathWithUnitOnBoardStillReplaysPilotingRolls() {
        MovePath movePath = createTankPath();

        UnitAction unitAction = assertDoesNotThrow(() -> UnitAction.fromMovePath(movePath));

        assertNotNull(unitAction.get(UnitAction.Field.CHANCE_OF_FAILURE));
    }
}

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.util.List;

import megamek.client.ui.SharedUtility;
import megamek.common.GameBoardTestCase;
import megamek.common.Player;
import megamek.common.enums.MoveStepType;
import megamek.common.moves.MovePath;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Tank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Regression tests for megamek #8972: building a dataset row from a finished move must not fail when the move left
 * the unit without a position (it mounted a DropShip, was recovered by a carrier, or left the board).
 */
class UnitActionFromMovePathTest extends GameBoardTestCase {

    private static final int FIRST_ROLL_TARGET = 8;
    private static final int SECOND_ROLL_TARGET = 9;

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
    @DisplayName("A unit left without a position records no failure chance and never replays the rolls")
    void fromMovePathWithLoadedUnitDoesNotThrow() {
        MovePath movePath = createTankPath();
        // The server loads the unit during processMovement, which clears its position before the path is logged
        movePath.getEntity().setPosition(null);

        try (MockedStatic<SharedUtility> sharedUtility = Mockito.mockStatic(SharedUtility.class)) {
            UnitAction unitAction = assertDoesNotThrow(() -> UnitAction.fromMovePath(movePath));

            assertEquals(1.0, unitAction.get(UnitAction.Field.CHANCE_OF_FAILURE),
                  "A move that needs no rolls records a failure chance of 1.0");
            // Without the guard this call happens and throws, because the replay starts from the missing position
            sharedUtility.verify(() -> SharedUtility.getPSRList(any(MovePath.class)), never());
        }
    }

    @Test
    @DisplayName("A unit still on the board replays its piloting rolls into the failure chance")
    void fromMovePathWithUnitOnBoardStillReplaysPilotingRolls() {
        MovePath movePath = createTankPath();
        List<TargetRoll> pilotingRolls = List.of(new TargetRoll(FIRST_ROLL_TARGET, "test roll"),
              new TargetRoll(SECOND_ROLL_TARGET, "test roll"));

        try (MockedStatic<SharedUtility> sharedUtility = Mockito.mockStatic(SharedUtility.class)) {
            sharedUtility.when(() -> SharedUtility.getPSRList(any(MovePath.class))).thenReturn(pilotingRolls);

            UnitAction unitAction = assertDoesNotThrow(() -> UnitAction.fromMovePath(movePath));

            double expected = (FIRST_ROLL_TARGET / 36d) * (SECOND_ROLL_TARGET / 36d);
            assertEquals(expected, (Double) unitAction.get(UnitAction.Field.CHANCE_OF_FAILURE), 1e-9,
                  "Each roll's target number over 36 is multiplied into the failure chance");
            sharedUtility.verify(() -> SharedUtility.getPSRList(any(MovePath.class)), times(1));
        }
    }
}

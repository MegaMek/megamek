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
package megamek.common.moves;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.GameBoardTestCase;
import megamek.common.enums.MoveStepType;
import megamek.common.units.BipedMek;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Tank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests that a vehicle which is hull-down in a fortified ("infantry-built") hex forfeits its cover when it changes
 * facing in place, since RAW requires it to exit, turn, then re-enter rather than turning within the hex (TO:AUE). Meks
 * use the partial-cover hull-down rules and are unaffected.
 */
class HullDownFacingTest extends GameBoardTestCase {

    static {
        initializeBoard("FORTIFIED_BOARD", """
              size 2 2
              hex 0101 0 "fortified:1" ""
              hex 0201 0 "fortified:1" ""
              hex 0102 0 "" ""
              hex 0202 0 "" ""
              end""");
        initializeBoard("PLAIN_BOARD", """
              size 2 2
              hex 0101 0 "" ""
              hex 0201 0 "" ""
              hex 0102 0 "" ""
              hex 0202 0 "" ""
              end""");
    }

    @Test
    @DisplayName("A hull-down vehicle that turns in place in a fortified hex loses its hull-down cover")
    void hullDownVehicleLosesCoverWhenTurningInFortifiedHex() {
        setBoard("FORTIFIED_BOARD");
        Tank tank = new Tank();
        tank.setHullDown(true);

        MovePath movePath = getMovePathFor(tank, EntityMovementMode.TRACKED, MoveStepType.TURN_LEFT);

        assertFalse(movePath.getLastStep().isHullDown(),
              "Turning in place within a fortified hex should forfeit hull-down cover");
    }

    @Test
    @DisplayName("A hull-down vehicle that drives out of a fortified hex loses its hull-down cover")
    void hullDownVehicleLosesCoverWhenDrivingOut() {
        setBoard("FORTIFIED_BOARD");
        Tank tank = new Tank();
        tank.setHullDown(true);

        MovePath movePath = getMovePathFor(tank, EntityMovementMode.TRACKED, MoveStepType.FORWARDS);

        assertFalse(movePath.getLastStep().isHullDown(),
              "Driving out of the hex should forfeit hull-down cover (existing behavior)");
    }

    @Test
    @DisplayName("A hull-down vehicle turning where there is no fortified terrain keeps its state (gate is fortified)")
    void hullDownVehicleTurningOutsideFortifiedHexIsNotAffected() {
        setBoard("PLAIN_BOARD");
        Tank tank = new Tank();
        tank.setHullDown(true);

        MovePath movePath = getMovePathFor(tank, EntityMovementMode.TRACKED, MoveStepType.TURN_LEFT);

        assertTrue(movePath.getLastStep().isHullDown(),
              "The in-place-turn rule is gated on fortified terrain, so it should not fire on a plain hex");
    }

    @Test
    @DisplayName("A hull-down Mek that turns in place in a fortified hex keeps its hull-down cover")
    void hullDownMekKeepsCoverWhenTurningInFortifiedHex() {
        setBoard("FORTIFIED_BOARD");
        BipedMek mek = new BipedMek();
        mek.setHullDown(true);

        MovePath movePath = getMovePathFor(mek, MoveStepType.TURN_LEFT);

        assertTrue(movePath.getLastStep().isHullDown(),
              "Meks use the partial-cover hull-down rules and may change facing without losing cover");
    }
}

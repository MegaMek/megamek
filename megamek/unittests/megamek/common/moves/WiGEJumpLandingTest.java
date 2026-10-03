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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.GameBoardTestCase;
import megamek.common.enums.MoveStepType;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.LandAirMek;
import megamek.common.units.Mek;
import megamek.common.units.SupportTank;
import org.junit.jupiter.api.Test;

/**
 * A WiGE vehicle has landed at the end of a jump and must take off again next turn (rules answer, forum topic 68110).
 * It lands only in a clear, paved or water hex and crashes anywhere else (TW p.55).
 */
public class WiGEJumpLandingTest extends GameBoardTestCase {

    static {
        initializeBoard("WIGE_JUMP_HILL", """
              size 1 2
              hex 0101 0 "" ""
              hex 0102 2 "" ""
              end""");
        initializeBoard("WIGE_JUMP_CLEAR", """
              size 1 2
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              end""");
        initializeBoard("WIGE_JUMP_WATER", """
              size 1 2
              hex 0101 0 "" ""
              hex 0102 0 "water:2" ""
              end""");
        initializeBoard("WIGE_JUMP_ROUGH", """
              size 1 2
              hex 0101 0 "" ""
              hex 0102 0 "rough:1" ""
              end""");
        initializeBoard("WIGE_JUMP_WOODS", """
              size 1 2
              hex 0101 0 "" ""
              hex 0102 0 "woods:1;foliage_elev:2" ""
              end""");
    }

    private MovePath jumpOneHex(String board) {
        setBoard(board);
        return getMovePathFor(new SupportTank(), EntityMovementMode.WIGE, MoveStepType.START_JUMP,
              MoveStepType.FORWARDS);
    }

    @Test
    void jumpOntoHillEndsOnTheGround() {
        MovePath movePath = jumpOneHex("WIGE_JUMP_HILL");

        assertTrue(movePath.isMoveLegal());
        assertEquals(0, movePath.getFinalElevation(), "The WiGE has landed on the hill, not 1 level above it");
        assertFalse(movePath.landsWiGEVehicleWhereItCrashes(), "A clear hex is a safe landing");
    }

    @Test
    void jumpIntoWaterFloatsOnTheSurface() {
        MovePath movePath = jumpOneHex("WIGE_JUMP_WATER");

        assertTrue(movePath.isMoveLegal());
        assertEquals(0, movePath.getFinalElevation(), "All WiGEs float on water (TW p.55)");
        assertFalse(movePath.landsWiGEVehicleWhereItCrashes(), "Water is a safe landing");
    }

    @Test
    void jumpIntoRoughCrashes() {
        MovePath movePath = jumpOneHex("WIGE_JUMP_ROUGH");

        assertTrue(movePath.landsWiGEVehicleWhereItCrashes(), "A WiGE that lands in rough crashes (TW p.55)");
    }

    @Test
    void jumpIntoWoodsCrashes() {
        MovePath movePath = jumpOneHex("WIGE_JUMP_WOODS");

        assertTrue(movePath.landsWiGEVehicleWhereItCrashes(), "A WiGE that lands in woods crashes (TW p.55)");
    }

    @Test
    void landAirMekKeepsFlyingAfterAJump() {
        setBoard("WIGE_JUMP_CLEAR");
        MovePath movePath = getMovePathFor(new LandAirMek(Mek.GYRO_STANDARD, Mek.COCKPIT_STANDARD,
              LandAirMek.LAM_STANDARD), EntityMovementMode.WIGE, MoveStepType.START_JUMP, MoveStepType.FORWARDS);

        assertEquals(1, movePath.getFinalElevation(), "The jump ruling covers WiGE vehicles, not AirMeks");
    }
}

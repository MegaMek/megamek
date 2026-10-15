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
 * TO:AUE p.162: a WiGE vehicle may fire its jump jets only while airborne, and the jump always returns it to its
 * standard one elevation above the terrain it comes down on.
 */
public class WiGEJumpTest extends GameBoardTestCase {

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
        initializeBoard("WIGE_JUMP_WOODS", """
              size 1 2
              hex 0101 0 "" ""
              hex 0102 0 "woods:1;foliage_elev:2" ""
              end""");
    }

    private MovePath jumpOneHex(String board, int startingElevation) {
        setBoard(board);
        return getMovePathFor(new SupportTank(), startingElevation, EntityMovementMode.WIGE, MoveStepType.START_JUMP,
              MoveStepType.FORWARDS);
    }

    @Test
    void jumpOntoHillEndsOneLevelAboveIt() {
        MovePath movePath = jumpOneHex("WIGE_JUMP_HILL", 1);

        assertTrue(movePath.isMoveLegal());
        assertEquals(1, movePath.getFinalElevation(), "One level above the level 2 hill, not above level 0");
    }

    @Test
    void jumpOntoClearGroundStaysAirborne() {
        MovePath movePath = jumpOneHex("WIGE_JUMP_CLEAR", 1);

        assertTrue(movePath.isMoveLegal());
        assertEquals(1, movePath.getFinalElevation(), "The jump returns the WiGE to one level up");
    }

    @Test
    void jumpOverWaterStaysAboveTheSurface() {
        MovePath movePath = jumpOneHex("WIGE_JUMP_WATER", 1);

        assertTrue(movePath.isMoveLegal());
        assertEquals(1, movePath.getFinalElevation(), "One level above the water surface");
    }

    @Test
    void cannotJumpIntoWoods() {
        MovePath movePath = jumpOneHex("WIGE_JUMP_WOODS", 1);

        assertFalse(movePath.isMoveLegal(), "One level up in woods is below the treetops, which a WiGE may not enter");
    }

    @Test
    void groundedWiGECannotJump() {
        MovePath movePath = jumpOneHex("WIGE_JUMP_CLEAR", 0);

        assertFalse(movePath.isMoveLegal(), "A WiGE may fire its jump jets only while airborne (TO:AUE p.162)");
    }

    @Test
    void landAirMekJumpIsUnchanged() {
        setBoard("WIGE_JUMP_CLEAR");
        MovePath movePath = getMovePathFor(new LandAirMek(Mek.GYRO_STANDARD, Mek.COCKPIT_STANDARD,
              LandAirMek.LAM_STANDARD), EntityMovementMode.WIGE, MoveStepType.START_JUMP, MoveStepType.FORWARDS);

        assertEquals(1, movePath.getFinalElevation(), "AirMeks keep their own jump behaviour");
    }
}

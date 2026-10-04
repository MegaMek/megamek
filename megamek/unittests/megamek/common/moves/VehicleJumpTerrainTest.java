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
import megamek.common.units.SupportTank;
import org.junit.jupiter.api.Test;

/**
 * TO:AUE p.162: a vehicle with jump jets jumps like a 'Mek, so it comes down on the ground of the hex it lands in,
 * and it may not jump into terrain its motive type forbids.
 */
public class VehicleJumpTerrainTest extends GameBoardTestCase {

    static {
        initializeBoard("VEHICLE_JUMP_HILL", """
              size 1 2
              hex 0101 0 "" ""
              hex 0102 2 "" ""
              end""");
        initializeBoard("VEHICLE_JUMP_WATER", """
              size 1 2
              hex 0101 0 "" ""
              hex 0102 0 "water:2" ""
              end""");
        initializeBoard("VEHICLE_JUMP_WOODS", """
              size 1 2
              hex 0101 0 "" ""
              hex 0102 0 "woods:1;foliage_elev:2" ""
              end""");
    }

    private MovePath jumpOneHex(String board, EntityMovementMode mode) {
        setBoard(board);
        return getMovePathFor(new SupportTank(), 0, mode, MoveStepType.START_JUMP, MoveStepType.FORWARDS);
    }

    @Test
    void hovercraftJumpOntoHillEndsOnTheGround() {
        MovePath movePath = jumpOneHex("VEHICLE_JUMP_HILL", EntityMovementMode.HOVER);

        assertTrue(movePath.isMoveLegal());
        assertEquals(0, movePath.getFinalElevation(), "On the level 2 hill, not 2 levels above it");
    }

    @Test
    void hovercraftJumpOntoWaterEndsOnTheSurface() {
        MovePath movePath = jumpOneHex("VEHICLE_JUMP_WATER", EntityMovementMode.HOVER);

        assertTrue(movePath.isMoveLegal());
        assertEquals(0, movePath.getFinalElevation(), "A hovercraft rides on the water surface");
    }

    @Test
    void hovercraftCannotJumpIntoWoods() {
        MovePath movePath = jumpOneHex("VEHICLE_JUMP_WOODS", EntityMovementMode.HOVER);

        assertFalse(movePath.isMoveLegal(), "Hovercraft may not enter woods, so they may not jump into them");
    }

    @Test
    void trackedVehicleCanJumpIntoLightWoods() {
        MovePath movePath = jumpOneHex("VEHICLE_JUMP_WOODS", EntityMovementMode.TRACKED);

        assertTrue(movePath.isMoveLegal(), "Tracked vehicles may enter light woods");
        assertEquals(0, movePath.getFinalElevation());
    }
}

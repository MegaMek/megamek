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
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.GameBoardTestCase;
import megamek.common.enums.MoveStepType;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.SupportTank;
import org.junit.jupiter.api.Test;

/**
 * Issue #7041: an airborne WiGE flies one elevation above the ground, which is exactly the level of a low bridge deck
 * (bridge elevation 1). That step was illegal, so a WiGE could only cross such a bridge when it arrived high enough,
 * e.g. holding altitude off higher ground on one side, which made it look like bridges could only be crossed from one
 * direction. The WiGE now flies over a deck at its own level, paying the +2 MP for flying more than one level above the
 * ground. Units start at 0101 facing south, so each FORWARDS step enters the next hex down the board.
 */
class WiGEBridgeTest extends GameBoardTestCase {

    private static final String LOW_BRIDGE = "bridge:1:09;bridge_cf:250;bridge_elev:1";

    static {
        initializeBoard("FLAT_LOW_BRIDGE", """
              size 1 5
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 0 "%s" ""
              hex 0104 0 "" ""
              hex 0105 0 "" ""
              end""".formatted(LOW_BRIDGE));

        // Higher ground before the bridge (the direction that already worked)
        initializeBoard("HIGH_GROUND_BEFORE_BRIDGE", """
              size 1 4
              hex 0101 1 "" ""
              hex 0102 1 "" ""
              hex 0103 0 "%s" ""
              hex 0104 0 "" ""
              end""".formatted(LOW_BRIDGE));

        // The same bridge approached from the low side (the direction that failed)
        initializeBoard("HIGH_GROUND_AFTER_BRIDGE", """
              size 1 4
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 0 "%s" ""
              hex 0104 1 "" ""
              end""".formatted(LOW_BRIDGE));

        initializeBoard("HIGH_BRIDGE", """
              size 1 3
              hex 0101 0 "" ""
              hex 0102 0 "bridge:1:09;bridge_cf:250;bridge_elev:3" ""
              hex 0103 0 "" ""
              end""");
    }

    private MovePath flyForwards(int steps, MoveStepType... firstSteps) {
        MoveStepType[] allSteps = new MoveStepType[firstSteps.length + steps];
        System.arraycopy(firstSteps, 0, allSteps, 0, firstSteps.length);
        for (int i = firstSteps.length; i < allSteps.length; i++) {
            allSteps[i] = MoveStepType.FORWARDS;
        }
        return getMovePathFor(new SupportTank(), 1, EntityMovementMode.WIGE, allSteps);
    }

    @Test
    void fliesOverLowBridgeOnFlatGround() {
        setBoard("FLAT_LOW_BRIDGE");
        MovePath path = flyForwards(4, MoveStepType.CLIMB_MODE_OFF);

        assertTrue(path.isMoveLegal(), "an airborne WiGE must be able to cross a low bridge");
        assertMovePathElevations(path, 1, 1, 2, 1, 1);
        assertEquals(6, path.getMpUsed(), "flying over the deck costs +2 MP for that hex");
    }

    @Test
    void fliesOverLowBridgeOnFlatGroundWithKeepElevation() {
        setBoard("FLAT_LOW_BRIDGE");
        MovePath path = flyForwards(4, MoveStepType.CLIMB_MODE_ON);

        assertTrue(path.isMoveLegal(), "Keep Elevation must not stop a WiGE crossing a low bridge");
        assertMovePathElevations(path, 1, 1, 2, 2, 2);
    }

    @Test
    void crossesFromTheHighSide() {
        setBoard("HIGH_GROUND_BEFORE_BRIDGE");
        MovePath path = flyForwards(3, MoveStepType.CLIMB_MODE_OFF);

        assertTrue(path.isMoveLegal(), "coming off higher ground, the WiGE flies over the deck");
        assertMovePathElevations(path, 1, 1, 2, 1);
    }

    @Test
    void crossesFromTheLowSide() {
        setBoard("HIGH_GROUND_AFTER_BRIDGE");
        MovePath path = flyForwards(3, MoveStepType.CLIMB_MODE_OFF);

        assertTrue(path.isMoveLegal(), "from the low side, the WiGE must also be able to cross");
        assertMovePathElevations(path, 1, 1, 2, 1);
    }

    @Test
    void crossesFromTheLowSideWithKeepElevation() {
        setBoard("HIGH_GROUND_AFTER_BRIDGE");
        MovePath path = flyForwards(3, MoveStepType.CLIMB_MODE_ON);

        assertTrue(path.isMoveLegal(), "from the low side with Keep Elevation, the WiGE must also be able to cross");
    }

    @Test
    void fliesUnderAHighBridge() {
        setBoard("HIGH_BRIDGE");
        MovePath path = flyForwards(2, MoveStepType.CLIMB_MODE_OFF);

        assertTrue(path.isMoveLegal(), "a WiGE flies under a bridge well above it");
        assertMovePathElevations(path, 1, 1, 1);
        assertEquals(2, path.getMpUsed(), "flying under a bridge costs no extra MP");
    }
}

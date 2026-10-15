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
 * An airborne WiGE flies one level above the ground, below the treetops, so it may not enter a woods hex unless it
 * follows a road (TW p.55). A WiGE holding a higher elevation above the trees may enter the woods hex (Movement Costs
 * Table, TW p.52, note 12).
 */
public class WiGEWoodsEntryTest extends GameBoardTestCase {

    static {
        initializeBoard("WIGE_WOODS_FLAT", """
              size 1 2
              hex 0101 0 "" ""
              hex 0102 0 "woods:1;foliage_elev:2" ""
              end""");
        initializeBoard("WIGE_WOODS_BELOW_HILL", """
              size 1 2
              hex 0101 2 "" ""
              hex 0102 0 "woods:1;foliage_elev:2" ""
              end""");
        initializeBoard("WIGE_WOODS_ROAD", """
              size 1 2
              hex 0101 0 "road:1" ""
              hex 0102 0 "woods:1;foliage_elev:2;road:1" ""
              end""");
    }

    private MovePath flyInto(String board, MoveStepType... steps) {
        setBoard(board);
        return getMovePathFor(new SupportTank(), 1, EntityMovementMode.WIGE, steps);
    }

    @Test
    void cannotFlyIntoWoodsBelowTheTreetops() {
        MovePath movePath = flyInto("WIGE_WOODS_FLAT", MoveStepType.FORWARDS);

        assertFalse(movePath.isMoveLegal(), "At one level up the WiGE is below the treetops (TW p.55)");
    }

    @Test
    void canFlyIntoWoodsAlongARoad() {
        MovePath movePath = flyInto("WIGE_WOODS_ROAD", MoveStepType.FORWARDS);

        assertTrue(movePath.isMoveLegal(), "A WiGE may follow a road through woods (TW p.55)");
    }

    @Test
    void canHoldAltitudeOverTheTreetops() {
        MovePath movePath = flyInto("WIGE_WOODS_BELOW_HILL", MoveStepType.CLIMB_MODE_ON, MoveStepType.FORWARDS);

        assertTrue(movePath.isMoveLegal(), "Holding elevation 3 over level 0 woods keeps it above the trees");
        assertEquals(3, movePath.getFinalElevation(), "It keeps its elevation coming off the level 2 hill");
    }

    @Test
    void cannotDescendIntoTheTreetopsOffAHill() {
        MovePath movePath = flyInto("WIGE_WOODS_BELOW_HILL", MoveStepType.CLIMB_MODE_OFF, MoveStepType.FORWARDS);

        assertFalse(movePath.isMoveLegal(), "Dropping to one level over the woods puts it in the trees");
    }
}

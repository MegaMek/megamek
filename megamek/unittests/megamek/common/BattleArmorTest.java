/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.client.ui.SharedUtility;
import megamek.common.moves.MovePath;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Test class for BattleArmor.
 * @author Luana Coppio
 */
public class BattleArmorTest extends GameBoardTestCase {

    @Nested
    class AntiMekSkillRollNag {
        static {
            initializeBoard("ROLL_ANTI_MEK_TO_ENTER", """
size 1 6
hex 0101 0 "" ""
hex 0102 0 "bldg_elev:6;building:2:8;bldg_cf:100" ""
hex 0103 0 "bldg_elev:6;building:2:9;bldg_cf:100" ""
hex 0104 0 "bldg_elev:6;building:2:9;bldg_cf:100" ""
hex 0105 0 "bldg_elev:6;building:2:9;bldg_cf:100" ""
hex 0106 0 "bldg_elev:6;building:2:1;bldg_cf:100" ""
end""");
            initializeBoard("ROLL_ANTI_MEK_TO_ENTER_TALL_BUILDINGS", """
size 1 6
hex 0101 0 "" ""
hex 0102 0 "bldg_elev:60;building:2:8;bldg_cf:100" ""
hex 0103 0 "bldg_elev:60;building:2:9;bldg_cf:100" ""
hex 0104 0 "bldg_elev:60;building:2:9;bldg_cf:100" ""
hex 0105 0 "bldg_elev:60;building:2:9;bldg_cf:100" ""
hex 0106 0 "bldg_elev:60;building:2:1;bldg_cf:100" ""
end""");

            initializeBoard("ROLL_ANTI_MEK_TO_ENTER_LOWER_BUILDINGS", """
size 1 6
hex 0101 10 "" ""
hex 0102 0 "" ""
hex 0103 0 "bldg_elev:6;building:2:8;bldg_cf:100" ""
hex 0104 0 "bldg_elev:6;building:2:9;bldg_cf:100" ""
hex 0105 0 "bldg_elev:6;building:2:9;bldg_cf:100" ""
hex 0106 0 "bldg_elev:6;building:2:1;bldg_cf:100" ""
end""");
        }

        @Test
        void jumpingIntoBuildingThroughWindowRequiresRoll() {
            setBoard("ROLL_ANTI_MEK_TO_ENTER");
            MovePath movePath = getMovePathFor(new BattleArmor(),
                  EntityMovementMode.INF_LEG,
                  MovePath.MoveStepType.START_JUMP,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.DOWN,
                  MovePath.MoveStepType.DOWN,
                  MovePath.MoveStepType.DOWN
            );

            assertTrue(movePath.isMoveLegal(),
                  "A BA or infantry can only jump from inside a building to outside of it, or from out to in");
            assertMovePathElevations(movePath,0, 6, 5, 4, 3);

            String check = SharedUtility.doPSRCheck(movePath);
            assertFalse(check.isBlank(), "it should require a roll to jump into the building through the window");
        }

        @Test
        void jumpingIntoTallBuildingThroughWindowRequiresRoll() {
            setBoard("ROLL_ANTI_MEK_TO_ENTER_TALL_BUILDINGS");
            MovePath movePath = getMovePathFor(new BattleArmor(),
                  EntityMovementMode.INF_LEG,
                  MovePath.MoveStepType.START_JUMP,
                  MovePath.MoveStepType.FORWARDS
            );

            assertTrue(movePath.isMoveLegal(),
                  "A BA or infantry can only jump from inside a building to outside of it, or from out to in");
            assertMovePathElevations(movePath, 0, 8);

            String check = SharedUtility.doPSRCheck(movePath);
            assertFalse(check.isBlank(), "it should require a roll to jump into the building through the window");
        }

        @Test
        void jumpingIntoShorterBuildingThroughWindowRequiresRoll() {
            setBoard("ROLL_ANTI_MEK_TO_ENTER_LOWER_BUILDINGS");
            MovePath movePath = getMovePathFor(new BattleArmor(),
                  EntityMovementMode.INF_LEG,
                  MovePath.MoveStepType.START_JUMP,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.DOWN
            );

            assertTrue(movePath.isMoveLegal(),
                  "A BA or infantry can only jump from inside a building to outside of it, or from out to in");
            assertMovePathElevations(movePath, 10, 0, 6, 5);

            String check = SharedUtility.doPSRCheck(movePath);
            assertFalse(check.isBlank(), "It should require a roll to jump into the building through the window");
        }
    }

}

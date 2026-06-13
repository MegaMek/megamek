/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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

import megamek.client.ui.SharedUtility;
import megamek.common.GameBoardTestCase;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.enums.MoveStepType;
import megamek.common.units.EntityMovementMode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Test class for BattleArmor.
 *
 * @author Luana Coppio
 */
public class BattleArmorTest extends GameBoardTestCase {

    /**
     * Tests for {@link BattleArmor} jumping into each floor of a 4-story building.
     * <p>
     * Setup: the BA is two hexes away from the building (one empty hex between them) and has a jump MP of 3. DOWN and
     * UP steps after the horizontal movement select the target floor.
     *
     * @see MovePath#isMoveLegal()
     */
    @Nested
    class JumpingIntoBuildingFloors {
        static {
            initializeBoard("BA_JUMP_4_STORY_BUILDING", """
                  size 1 3
                  hex 0101 0 "" ""
                  hex 0102 0 "" ""
                  hex 0103 0 "bldg_elev:4;building:2:8;bldg_cf:100" ""
                  end""");
        }

        private BattleArmor ba() {
            BattleArmor ba = new BattleArmor();
            ba.setOriginalJumpMP(3);
            return ba;
        }

        @Test
        void jumpToElevation4() {
            // arrange
            setBoard("BA_JUMP_4_STORY_BUILDING");
            // act
            MovePath movePath = getMovePathFor(ba(),
                  EntityMovementMode.INF_LEG,
                  MoveStepType.START_JUMP,
                  MoveStepType.FORWARDS,
                  MoveStepType.FORWARDS,
                  MoveStepType.UP
            );
            // assert
            assertFalse(movePath.isMoveLegal());
        }

        @Test
        void jumpToElevation3() {
            // arrange
            setBoard("BA_JUMP_4_STORY_BUILDING");
            // act
            MovePath movePath = getMovePathFor(ba(),
                  EntityMovementMode.INF_LEG,
                  MoveStepType.START_JUMP,
                  MoveStepType.FORWARDS,
                  MoveStepType.FORWARDS
            );
            // assert
            assertTrue(movePath.isMoveLegal());
        }

        @Test
        void jumpToElevation2() {
            // arrange
            setBoard("BA_JUMP_4_STORY_BUILDING");
            // act
            MovePath movePath = getMovePathFor(ba(),
                  EntityMovementMode.INF_LEG,
                  MoveStepType.START_JUMP,
                  MoveStepType.FORWARDS,
                  MoveStepType.FORWARDS,
                  MoveStepType.DOWN
            );
            // assert
            assertTrue(movePath.isMoveLegal());
        }

        @Test
        void jumpToElevation1() {
            // arrange
            setBoard("BA_JUMP_4_STORY_BUILDING");
            // act
            MovePath movePath = getMovePathFor(ba(),
                  EntityMovementMode.INF_LEG,
                  MoveStepType.START_JUMP,
                  MoveStepType.FORWARDS,
                  MoveStepType.FORWARDS,
                  MoveStepType.DOWN,
                  MoveStepType.DOWN
            );
            // assert
            assertTrue(movePath.isMoveLegal());
        }

        @Test
        void jumpToElevation0() {
            // arrange
            setBoard("BA_JUMP_4_STORY_BUILDING");
            // act
            MovePath movePath = getMovePathFor(ba(),
                  EntityMovementMode.INF_LEG,
                  MoveStepType.START_JUMP,
                  MoveStepType.FORWARDS,
                  MoveStepType.FORWARDS,
                  MoveStepType.DOWN,
                  MoveStepType.DOWN,
                  MoveStepType.DOWN
            );
            // assert
            assertFalse(movePath.isMoveLegal());
        }
    }

    /**
     * Tests for {@link BattleArmor} jumping into each floor of a 4-story building on terrain at level 1.
     * <p>
     * Identical setup to {@link JumpingIntoBuildingFloors} except all hex terrain levels are 1.
     *
     * @see MovePath#isMoveLegal()
     */
    @Nested
    class JumpingIntoBuildingFloorsOnElevatedTerrain {
        static {
            initializeBoard("BA_JUMP_4_STORY_BUILDING_L1", """
                  size 1 3
                  hex 0101 1 "" ""
                  hex 0102 1 "" ""
                  hex 0103 1 "bldg_elev:4;building:2:8;bldg_cf:100" ""
                  end""");
        }

        private BattleArmor ba() {
            BattleArmor ba = new BattleArmor();
            ba.setOriginalJumpMP(3);
            return ba;
        }

        @Test
        void jumpToElevation4() {
            // arrange
            setBoard("BA_JUMP_4_STORY_BUILDING_L1");
            // act
            MovePath movePath = getMovePathFor(ba(),
                  EntityMovementMode.INF_LEG,
                  MoveStepType.START_JUMP,
                  MoveStepType.FORWARDS,
                  MoveStepType.FORWARDS,
                  MoveStepType.UP
            );
            // assert
            assertFalse(movePath.isMoveLegal());
        }

        @Test
        void jumpToElevation3() {
            // arrange
            setBoard("BA_JUMP_4_STORY_BUILDING_L1");
            // act
            MovePath movePath = getMovePathFor(ba(),
                  EntityMovementMode.INF_LEG,
                  MoveStepType.START_JUMP,
                  MoveStepType.FORWARDS,
                  MoveStepType.FORWARDS
            );
            // assert
            assertTrue(movePath.isMoveLegal());
        }

        @Test
        void jumpToElevation2() {
            // arrange
            setBoard("BA_JUMP_4_STORY_BUILDING_L1");
            // act
            MovePath movePath = getMovePathFor(ba(),
                  EntityMovementMode.INF_LEG,
                  MoveStepType.START_JUMP,
                  MoveStepType.FORWARDS,
                  MoveStepType.FORWARDS,
                  MoveStepType.DOWN
            );
            // assert
            assertTrue(movePath.isMoveLegal());
        }

        @Test
        void jumpToElevation1() {
            // arrange
            setBoard("BA_JUMP_4_STORY_BUILDING_L1");
            // act
            MovePath movePath = getMovePathFor(ba(),
                  EntityMovementMode.INF_LEG,
                  MoveStepType.START_JUMP,
                  MoveStepType.FORWARDS,
                  MoveStepType.FORWARDS,
                  MoveStepType.DOWN,
                  MoveStepType.DOWN
            );
            // assert
            assertTrue(movePath.isMoveLegal());
        }


        @Test
        void jumpToElevation0() {
            // arrange
            setBoard("BA_JUMP_4_STORY_BUILDING_L1");
            // act
            MovePath movePath = getMovePathFor(ba(),
                  EntityMovementMode.INF_LEG,
                  MoveStepType.START_JUMP,
                  MoveStepType.FORWARDS,
                  MoveStepType.FORWARDS,
                  MoveStepType.DOWN,
                  MoveStepType.DOWN,
                  MoveStepType.DOWN
            );
            // assert
            assertFalse(movePath.isMoveLegal());
        }
    }

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
                  MoveStepType.START_JUMP,
                  MoveStepType.FORWARDS,
                  MoveStepType.DOWN,
                  MoveStepType.DOWN,
                  MoveStepType.DOWN
            );

            assertTrue(movePath.isMoveLegal(),
                  "A BA or infantry can only jump from inside a building to outside of it, or from out to in");
            assertMovePathElevations(movePath, 0, 6, 5, 4, 3);

            String check = SharedUtility.doPSRCheck(movePath);
            assertFalse(check.isBlank(), "it should require a roll to jump into the building through the window");
        }

        @Test
        void jumpingIntoTallBuildingThroughWindowRequiresRoll() {
            setBoard("ROLL_ANTI_MEK_TO_ENTER_TALL_BUILDINGS");
            MovePath movePath = getMovePathFor(new BattleArmor(),
                  EntityMovementMode.INF_LEG,
                  MoveStepType.START_JUMP,
                  MoveStepType.FORWARDS
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
                  MoveStepType.START_JUMP,
                  MoveStepType.FORWARDS,
                  MoveStepType.FORWARDS,
                  MoveStepType.DOWN
            );

            assertTrue(movePath.isMoveLegal(),
                  "A BA or infantry can only jump from inside a building to outside of it, or from out to in");
            assertMovePathElevations(movePath, 0, 0, 6, 5);

            String check = SharedUtility.doPSRCheck(movePath);
            assertFalse(check.isBlank(), "It should require a roll to jump into the building through the window");
        }
    }

}

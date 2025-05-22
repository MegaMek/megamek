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
package megamek.common.moves;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.BattleArmor;
import megamek.common.BipedMek;
import megamek.common.EntityMovementMode;
import megamek.common.GameBoardTestCase;
import megamek.common.SupportTank;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Test class for MovePath executing JUMP the Jump movement type.
 * @author Luana Coppio
 */
public class JumpTest extends GameBoardTestCase {

    @Nested
    class JumpIntoBuilding {

        static {
            initializeBoard("BOARD_1x6_START_LOW", """
size 1 6
hex 0101 -1 "" ""
hex 0102 0 "" ""
hex 0103 0 "bldg_elev:2;building:2:8;bldg_cf:50" ""
hex 0104 0 "bldg_elev:4;building:2:8;bldg_cf:100" ""
hex 0105 0 "bldg_elev:4;building:2:9;bldg_cf:100" ""
hex 0106 0 "bldg_elev:4;building:2:1;bldg_cf:100" ""
end""");

            initializeBoard("BOARD_MORE_FORGIVING", """
size 1 6
hex 0101 0 "" ""
hex 0102 0 "" ""
hex 0103 0 "bldg_elev:2;building:2:8;bldg_cf:50" ""
hex 0104 0 "bldg_elev:4;building:2:8;bldg_cf:100" ""
hex 0105 0 "bldg_elev:4;building:2:9;bldg_cf:100" ""
hex 0106 0 "bldg_elev:4;building:2:1;bldg_cf:100" ""
end""");

            initializeBoard("BOARD_INSIDE_BUILDING", """
size 1 4
hex 0101 0 "bldg_elev:90;building:2:8;bldg_cf:100" ""
hex 0102 0 "bldg_elev:90;building:2:9;bldg_cf:100" ""
hex 0103 0 "bldg_elev:90;building:2:9;bldg_cf:100" ""
hex 0104 0 "bldg_elev:90;building:2:1;bldg_cf:100" ""
end""");

            initializeBoard("BOARD_JUMP_OUT_OF_BUILDING", """
size 1 4
hex 0101 0 "bldg_elev:90;building:2:1;bldg_cf:100" ""
hex 0102 0 "" ""
hex 0103 0 "bldg_elev:90;building:2:8;bldg_cf:100" ""
hex 0104 0 "bldg_elev:90;building:2:9;bldg_cf:100" ""
end""");
        }

        @Test
        void canJumpFromInsideBuilding1ToBuilding2() {
            setBoard("BOARD_JUMP_OUT_OF_BUILDING");
            int startingElevation = 5;
            MovePath movePath = getMovePathFor(new BattleArmor(),
                  startingElevation,
                  EntityMovementMode.INF_LEG,
                  MovePath.MoveStepType.START_JUMP,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.DOWN,
                  MovePath.MoveStepType.DOWN,
                  MovePath.MoveStepType.DOWN,
                  MovePath.MoveStepType.DOWN,
                  MovePath.MoveStepType.DOWN,
                  MovePath.MoveStepType.DOWN,
                  MovePath.MoveStepType.DOWN,
                  MovePath.MoveStepType.DOWN
            );

            assertTrue(movePath.isMoveLegal(),
                  "A BA or infantry can only jump from inside a building to outside of it, or from out to in");

            assertMovePathElevations(movePath,5, 0, 13, 12, 11, 10, 9, 8, 7, 6, 5);
        }

        @Test
        void canJumpFromInsideBuildingToOutside() {
            setBoard("BOARD_JUMP_OUT_OF_BUILDING");
            int startingElevation = 5;
            MovePath movePath = getMovePathFor(new BattleArmor(),
                  startingElevation,
                  EntityMovementMode.INF_LEG,
                  MovePath.MoveStepType.START_JUMP,
                  MovePath.MoveStepType.FORWARDS
            );

            assertTrue(movePath.isMoveLegal(),
                  "A BA or infantry can only jump from inside a building to outside of it, or from out to in");

            assertMovePathElevations(movePath,5, 0);
        }


        @Test
        void cantJumpDeepInsideTheBuildingFromInsideItself() {
            setBoard("BOARD_INSIDE_BUILDING");
            int startingElevation = 2;
            MovePath movePath = getMovePathFor(new BattleArmor(),
                  startingElevation,
                  EntityMovementMode.INF_LEG,
                  MovePath.MoveStepType.START_JUMP,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS
            );

            assertFalse(movePath.isMoveLegal(),
                  "A BA or infantry can only jump from inside a building to outside of it, or from out to in");

            assertMovePathElevations(movePath,2, 2, 2, 2);
        }

        @Test
        void cantJumpUpAnyLevelInsideTheBuildingFromInsideItself() {
            setBoard("BOARD_INSIDE_BUILDING");
            int startingElevation = 2;
            MovePath movePath = getMovePathFor(new BattleArmor(),
                  startingElevation,
                  EntityMovementMode.INF_LEG,
                  MovePath.MoveStepType.START_JUMP,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.UP
            );

            assertFalse(movePath.isMoveLegal(),
                  "A BA or infantry can only jump from inside a building to outside of it, or from out to in");

            assertMovePathElevations(movePath,2, 2, 3);
        }

        @Test
        void jumpIntoTheBuildingThroughTheFrontWindowSuccess() {
            setBoard("BOARD_MORE_FORGIVING");
            MovePath movePath = getMovePathFor(new BattleArmor(),
                  EntityMovementMode.INF_LEG,
                  MovePath.MoveStepType.START_JUMP,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.DOWN
            );

            assertTrue(movePath.isMoveLegal(),
                  "A BA cannot jump into a building lower floors comming from a connected side and not on LOS");

            assertMovePathElevations(movePath,0, 0, 2, 4, 3);
        }

        @Test
        void cantJumpIntoTheBuildingThroughTheRoof() {
            setBoard("BOARD_1x6_START_LOW");
            MovePath movePath = getMovePathFor(new BattleArmor(),
                  EntityMovementMode.INF_LEG,
                  MovePath.MoveStepType.START_JUMP,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.DOWN
            );

            assertFalse(movePath.isMoveLegal(),
                  "A BA cannot jump into a building lower floors comming from a connected side and not on LOS");

            assertMovePathElevations(movePath, 0, 0, 2, 4, 4, 3);
        }

        @Test
        void cantJumpIntoTheBuildingThroughTheWindowWithNoLOS() {
            setBoard("BOARD_1x6_START_LOW");
            MovePath movePath = getMovePathFor(new BattleArmor(), EntityMovementMode.INF_LEG,
                  MovePath.MoveStepType.START_JUMP,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.DOWN
            );

            assertFalse(movePath.isMoveLegal(), "A BA can jump into a building lower floors coming from an exit side" +
                                                     " and if the LOS is unblocked");
            assertMovePathElevations(movePath, 0, 0, 2, 4, 3);
        }

        @Test
        void cantJumpIntoTheBuildingThroughTheWindowButItsOnSameLevelOfNeighboringBuilding() {
            setBoard("BOARD_1x6_START_LOW");
            MovePath movePath = getMovePathFor(new BattleArmor(), EntityMovementMode.INF_LEG,
                  MovePath.MoveStepType.START_JUMP,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.DOWN,
                  MovePath.MoveStepType.DOWN
            );

            assertFalse(movePath.isMoveLegal(), "A BA cant jump into a building lower floors coming from a side " +
                                                      "which is covered by another building of same height");

            assertMovePathElevations(movePath, 0, 0, 2, 4, 3, 2);
        }

        @Test
        void cantJumpIntoTheBuildingThroughTheWindowOnSouthComingFromNorth() {
            setBoard("BOARD_1x6_START_LOW");
            MovePath movePath = getMovePathFor(new BattleArmor(),
                  EntityMovementMode.INF_LEG,
                  MovePath.MoveStepType.START_JUMP,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.DOWN
            );

            assertFalse(movePath.isMoveLegal(),
                  "A BA cannot jump into a building lower floors coming from a connected side");

            assertMovePathElevations(movePath, 0, 0, 2, 4, 4, 4, 3);
        }

        @Test
        void cantJumpIntoTheBuildingInTheLastRoofTopHex() {
            setBoard("BOARD_1x6_START_LOW");
            MovePath movePath = getMovePathFor(new BattleArmor(),
                  EntityMovementMode.INF_LEG,
                  MovePath.MoveStepType.START_JUMP,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS
            );

            assertTrue(movePath.isMoveLegal(),
                  "This is a sanity check, this jump should work");
            assertMovePathElevations(movePath, 0, 0, 2, 4, 4, 4);
        }

        @Test
        void cantJumpIntoTheBuildingThroughTheBlockedWindow() {
            setBoard("BOARD_1x6_START_LOW");
            MovePath movePath = getMovePathFor(new BattleArmor(),
                  EntityMovementMode.INF_LEG,
                  MovePath.MoveStepType.START_JUMP,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.DOWN,
                  MovePath.MoveStepType.DOWN,
                  MovePath.MoveStepType.DOWN,
                  MovePath.MoveStepType.DOWN
            );
            assertFalse(movePath.isMoveLegal(),
                  "A BA cannot jump into a building lower floors comming from an exit side");
            assertMovePathElevations(movePath, 0, 0, 2, 4, 3, 2, 1, 0);
        }

        @Test
        void bipedMekJumpPath() {
            setBoard("BOARD_1x6_START_LOW");
            MovePath movePath = getMovePathFor(new BipedMek(), EntityMovementMode.BIPED,
                  MovePath.MoveStepType.START_JUMP,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.DOWN
            );

            assertFalse(movePath.isMoveLegal(), "A Mek cannot jump into a building lower floors");
            assertMovePathElevations(movePath, 0, 0, 2, 4, 4, 4, 3);
        }
    }

    @Nested
    class JumpOverBuildings {

        static {
            initializeBoard("BOARD_JUMP_INTO_BUILDING", """
size 1 5
hex 0101 0 "" ""
hex 0102 0 "building:4;bldg_cf:100;bldg_elev:4;bldg_basement_type:2" ""
hex 0103 0 "building:4;bldg_cf:100;bldg_elev:4;bldg_basement_type:2" ""
hex 0104 0 "ultra_sublevel:0" ""
hex 0105 0 "water:2" ""
end"""
            );
        }
        
        @Test
        void bipedMekJumpPath() {
            setBoard("BOARD_JUMP_INTO_BUILDING");
            MovePath movePath = getMovePathFor(new BipedMek(), EntityMovementMode.BIPED,
                  MovePath.MoveStepType.START_JUMP,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS);
            assertMovePathElevations(movePath,
                    ExpectedElevation.of(0, "Jumping from the ground, level 0"),
                    ExpectedElevation.of(4, "Jumping from the ground, level 0 with height 4"),
                    ExpectedElevation.of(4, "Jumping over a building, level 0 with height 4"),
                    ExpectedElevation.of(0, "Jumping over ultra sublevel, level 0"),
                    ExpectedElevation.of(-2, "Jumping into a water hex, level 0 with depth 2"));
        }

        @Test
        void wigeJumpMovePath() {
            setBoard("BOARD_JUMP_INTO_BUILDING");
            MovePath movePath = getMovePathFor(new SupportTank(), EntityMovementMode.WIGE,
                  MovePath.MoveStepType.START_JUMP,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS);
            assertMovePathElevations(movePath,
                  ExpectedElevation.of(1, "Jumping from the ground, level 0, wige +1 elevation"),
                  ExpectedElevation.of(5, "Jumping from the ground, level 0 with height 4, wige +1 elevation"),
                  ExpectedElevation.of(5, "Jumping over a building, level 0 with height 4, wige +1 elevation"),
                  ExpectedElevation.of(1, "Jumping over ultra sublevel, level 0, wige +1 elevation"),
                  ExpectedElevation.of(1, "Jumping into a water hex, level 0 with depth 2, wige stay above water " +
                                                "with 1 elevation"));
        }

        @Test
        void hoverJumpPath() {
            setBoard("BOARD_JUMP_INTO_BUILDING");
            MovePath movePath = getMovePathFor(new SupportTank(), EntityMovementMode.HOVER,
                  MovePath.MoveStepType.START_JUMP,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS);
            assertMovePathElevations(movePath,
                  ExpectedElevation.of(0, "Jumping from the ground, level 0"),
                  ExpectedElevation.of(4, "Jumping from the ground, level 0 with height 4"),
                  ExpectedElevation.of(4, "Jumping over a building, level 0 with height 4"),
                  ExpectedElevation.of(0, "Jumping over ultra sublevel, level 0"),
                  ExpectedElevation.of(0, "Jumping into a water hex, level 0 with depth 2, hover stay above water"));
        }
    }

    @Nested
    class JumpOverBridgesIntoBridge {

        static {
            initializeBoard("BOARD_MISC_TILES", """
size 1 5
hex 0101 0 "bridge:1;bridge_cf:100;bridge_elev:4" ""
hex 0102 0 "bldg_elev:4;building:2:0;bldg_cf:100;bldg_basement_type:2" ""
hex 0103 0 "ultra_sublevel:0;bridge:1;bridge_cf:100;bridge_elev:2" ""
hex 0104 0 "water:2" ""
hex 0105 0 "water:2;bridge:1;bridge_cf:100;bridge_elev:1" ""
end"""
            );
        }

        @Test
        void testJumpMovePath() {
            setBoard("BOARD_MISC_TILES");
            MovePath movePath = getMovePathFor(new BipedMek(), EntityMovementMode.BIPED,
                  MovePath.MoveStepType.START_JUMP,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS);
            assertMovePathElevations(movePath, 4, 4, 2, -2, 1);
        }

        @Test
        void wigeJumpMovePath() {
            setBoard("BOARD_MISC_TILES");
            MovePath movePath = getMovePathFor(new SupportTank(), EntityMovementMode.WIGE,
                  MovePath.MoveStepType.START_JUMP,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS);
            assertMovePathElevations(movePath, 5, 5, 3, 1, 2);
        }

        @Test
        void testHoverJumpMovePath() {
            setBoard("BOARD_MISC_TILES");
            MovePath movePath = getMovePathFor(new SupportTank(), EntityMovementMode.HOVER,
                  MovePath.MoveStepType.START_JUMP,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS);
            assertMovePathElevations(movePath, 4, 4, 2, 0, 1);
        }
    }
}

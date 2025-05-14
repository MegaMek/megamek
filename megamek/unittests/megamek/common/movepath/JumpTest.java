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
package megamek.common.movepath;

import megamek.common.*;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for MovePath executing JUMP the Jump movement type.
 * @author Luana Coppio
 */
public class JumpTest {

    @BeforeAll
    static void setUp() {
        EquipmentType.initializeTypes();
    }


    @Nested
    class JumpIntoBuilding {
        private static final Board BOARD;
        private static final Board BOARD_MORE_FORGIVING;
        private static final Board BOARD_INSIDE_BUILDING;
        private static final Board BOARD_JUMP_OUT_OF_BUILDING;

        static {
            BOARD = new Board(1, 6);
            BOARD.load("""
size 1 6
hex 0101 -1 "" ""
hex 0102 0 "" ""
hex 0103 0 "bldg_elev:2;building:2:8;bldg_cf:50" ""
hex 0104 0 "bldg_elev:4;building:2:8;bldg_cf:100" ""
hex 0105 0 "bldg_elev:4;building:2:9;bldg_cf:100" ""
hex 0106 0 "bldg_elev:4;building:2:1;bldg_cf:100" ""
end""");

            BOARD_MORE_FORGIVING = new Board(1, 6);
            BOARD_MORE_FORGIVING.load("""
size 1 6
hex 0101 0 "" ""
hex 0102 0 "" ""
hex 0103 0 "bldg_elev:2;building:2:8;bldg_cf:50" ""
hex 0104 0 "bldg_elev:4;building:2:8;bldg_cf:100" ""
hex 0105 0 "bldg_elev:4;building:2:9;bldg_cf:100" ""
hex 0106 0 "bldg_elev:4;building:2:1;bldg_cf:100" ""
end""");

            BOARD_INSIDE_BUILDING = new Board(1, 4);
            BOARD_INSIDE_BUILDING.load("""
size 1 4
hex 0101 0 "bldg_elev:90;building:2:8;bldg_cf:100" ""
hex 0102 0 "bldg_elev:90;building:2:9;bldg_cf:100" ""
hex 0103 0 "bldg_elev:90;building:2:9;bldg_cf:100" ""
hex 0104 0 "bldg_elev:90;building:2:1;bldg_cf:100" ""
end""");

            BOARD_JUMP_OUT_OF_BUILDING = new Board(1, 4);
            BOARD_JUMP_OUT_OF_BUILDING.load("""
size 1 4
hex 0101 0 "bldg_elev:90;building:2:1;bldg_cf:100" ""
hex 0102 0 "" ""
hex 0103 0 "bldg_elev:90;building:2:8;bldg_cf:100" ""
hex 0104 0 "bldg_elev:90;building:2:9;bldg_cf:100" ""
end""");
        }

        private Game game;

        @BeforeEach
        void setUpEach() {
            game = new Game();
            game.setBoard(BOARD_MORE_FORGIVING);
        }

        @Test
        void canJumpFromInsideBuilding1ToBuilding2() {
            game.setBoard(BOARD_JUMP_OUT_OF_BUILDING);
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

            assertMoveSteps(movePath,5, 0, 13, 12, 11, 10, 9, 8, 7, 6, 5);
        }

        @Test
        void canJumpFromInsideBuildingToOutside() {
            game.setBoard(BOARD_JUMP_OUT_OF_BUILDING);
            int startingElevation = 5;
            MovePath movePath = getMovePathFor(new BattleArmor(),
                  startingElevation,
                  EntityMovementMode.INF_LEG,
                  MovePath.MoveStepType.START_JUMP,
                  MovePath.MoveStepType.FORWARDS
            );

            assertTrue(movePath.isMoveLegal(),
                  "A BA or infantry can only jump from inside a building to outside of it, or from out to in");

            assertMoveSteps(movePath,5, 0);
        }


        @Test
        void cantJumpDeepInsideTheBuildingFromInsideItself() {
            game.setBoard(BOARD_INSIDE_BUILDING);
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

            assertMoveSteps(movePath,2, 2, 2, 2);
        }

        @Test
        void cantJumpUpAnyLevelInsideTheBuildingFromInsideItself() {
            game.setBoard(BOARD_INSIDE_BUILDING);
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

            assertMoveSteps(movePath,2, 2, 3);
        }

        @Test
        void jumpIntoTheBuildingThroughTheFrontWindowSuccess() {
            game.setBoard(BOARD_MORE_FORGIVING);
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

            assertMoveSteps(movePath,0, 0, 2, 4, 3);
        }

        @Test
        void cantJumpIntoTheBuildingThroughTheRoof() {
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

            assertMoveSteps(movePath, 0, 0, 2, 4, 4, 3);
        }

        @Test
        void cantJumpIntoTheBuildingThroughTheWindowWithNoLOS() {
            game.setBoard(BOARD);
            MovePath movePath = getMovePathFor(new BattleArmor(), EntityMovementMode.INF_LEG,
                  MovePath.MoveStepType.START_JUMP,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.DOWN
            );

            assertFalse(movePath.isMoveLegal(), "A BA can jump into a building lower floors coming from an exit side" +
                                                     " and if the LOS is unblocked");
            assertMoveSteps(movePath, -1, 0, 2, 4, 3);
        }

        @Test
        void cantJumpIntoTheBuildingThroughTheWindowButItsOnSameLevelOfNeighboringBuilding() {
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

            assertMoveSteps(movePath, 0, 0, 2, 4, 3, 2);
        }

        @Test
        void cantJumpIntoTheBuildingThroughTheWindowOnSouthComingFromNorth() {
            MovePath movePath = getMovePathFor(new BattleArmor(),
                  EntityMovementMode.INF_LEG,
                  MovePath.MoveStepType.START_JUMP,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.FORWARDS,
                  MovePath.MoveStepType.DOWN
                  // try to land in the middle of the building comming through the roof
            );

            assertFalse(movePath.isMoveLegal(),
                  "A BA cannot jump into a building lower floors comming from a connected side");

            assertMoveSteps(movePath, 0, 0, 2, 4, 4, 4, 3);
        }

        @Test
        void cantJumpIntoTheBuildingInTheLastRoofTopHex() {
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
            assertMoveSteps(movePath, 0, 0, 2, 4, 4, 4);
        }

        @Test
        void cantJumpIntoTheBuildingThroughTheBlockedWindow() {
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
            assertMoveSteps(movePath, 0, 0, 2, 4, 3, 2, 1, 0);
        }

        @Test
        void bipedMekJumpPath() {
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
            assertMoveSteps(movePath, 0, 0, 2, 4, 4, 4, 3);
        }

        private void assertMoveSteps(MovePath movePath, int ... expectedElevations) {
            for (int i = 0; i < movePath.getStepVector().size(); i++) {
                var hex = game.getBoard().getHex(movePath.getStepVector().get(i).getPosition());
                assertEquals(expectedElevations[i], movePath.getStepVector().get(i).getElevation(),
                      "Step " + i + ": " + new StepLog(movePath.getStepVector().get(i)) +
                            " hex " + hex.toString());
            }
        }

        private MovePath getMovePathFor(Entity entity, int startingElevation, EntityMovementMode movementMode,
              MovePath.MoveStepType ... steps) {
            return getMovePath(new MovePath(game, initializeUnit(entity, game, movementMode, startingElevation)), steps);
        }

        private MovePath getMovePathFor(Entity entity, EntityMovementMode movementMode,
              MovePath.MoveStepType ... steps) {
            return getMovePathFor(entity, Integer.MAX_VALUE,movementMode, steps);
        }
    }

    @Nested
    class JumpOverBuildings {
        private static final Board BOARD;
        static {
            Terrain[] fourStoresBuilding = new Terrain[] {
                  new Terrain(Terrains.BUILDING, 4), // building type medium
                  new Terrain(Terrains.BLDG_CF, 100), // building has CF 100
                  new Terrain(Terrains.BLDG_ELEV, 4),   // height of the building is 2
                  new Terrain(Terrains.BLDG_BASEMENT_TYPE, 2) // basement with depth 2
            };
            Terrain[] twoDepthWater = new Terrain[] {
                  new Terrain(Terrains.WATER, 2), // water with depth 2
            };
            Terrain[] ultraSubLevel = new Terrain[] {
                  new Terrain(Terrains.ULTRA_SUBLEVEL, 0) // I don't think the level here makes any difference
            };
            BOARD = new Board(1, 5);
            BOARD.setHex(0, 0, new Hex(0));
            BOARD.setHex(0, 1, new Hex(0, fourStoresBuilding, null));
            BOARD.setHex(0, 2, new Hex(0, fourStoresBuilding, null));
            BOARD.setHex(0, 3, new Hex(0, ultraSubLevel, null));
            BOARD.setHex(0, 4, new Hex(0, twoDepthWater, null));
        }

        private Game game;

        @BeforeEach
        void setUpEach() {
            game = new Game();
            game.setBoard(BOARD);
        }

        @Test
        void bipedMekJumpPath() {
            MovePath movePath = getMovePathFor(new BipedMek(), EntityMovementMode.BIPED);
            assertEquals(0, movePath.getStepVector().get(0).getElevation(),
                  "Jumping from the ground, level 0");
            assertEquals(4, movePath.getStepVector().get(1).getElevation(),
                  "Jumping over a building, level 0 with height 4");
            assertEquals(4, movePath.getStepVector().get(2).getElevation(),
                  "Jumping over a building, level 0 with height 4");
            assertEquals(0, movePath.getStepVector().get(3).getElevation(),
                  "Jumping over ultra sublevel, level 0");
            assertEquals(-2, movePath.getStepVector().get(4).getElevation(),
                  "Jumping into a water hex, level 0 with depth 2");

        }

        @Test
        void wigeJumpMovePath() {
            MovePath movePath = getMovePathFor(new SupportTank(), EntityMovementMode.WIGE);
            assertEquals(1, movePath.getStepVector().get(0).getElevation(),
                  "Jumping from the ground, level 0");
            assertEquals(5, movePath.getStepVector().get(1).getElevation(),
                  "Jumping over a building, level 0 with height 4");
            assertEquals(5, movePath.getStepVector().get(2).getElevation(),
                  "Jumping over a building, level 0 with height 4");
            assertEquals(1, movePath.getStepVector().get(3).getElevation(),
                  "Jumping over ultra sublevel, level 0");
            assertEquals(1, movePath.getStepVector().get(4).getElevation(),
                  "Jumping into a water hex, level 0 with depth 2, hover stay above water");
        }

        @Test
        void hoverJumpPath() {
            MovePath movePath = getMovePathFor(new SupportTank(), EntityMovementMode.HOVER);

            assertEquals(0, movePath.getStepVector().get(0).getElevation(),
                  "Jumping from the ground, level 0");
            assertEquals(4, movePath.getStepVector().get(1).getElevation(),
                  "Jumping over a building, level 0 with height 4");
            assertEquals(4, movePath.getStepVector().get(2).getElevation(),
                  "Jumping over a building, level 0 with height 4");
            assertEquals(0, movePath.getStepVector().get(3).getElevation(),
                  "Jumping over ultra sublevel, level 0");
            assertEquals(0, movePath.getStepVector().get(4).getElevation(),
                  "Jumping into a water hex, level 0 with depth 2, hover stay above water");
        }

        private MovePath getMovePathFor(Entity entity, EntityMovementMode movementMode) {
            return getMovePath(new MovePath(game, initializeUnit(entity, game, movementMode)));
        }
    }

    @Nested
    class JumpOverBridgesIntoBridge {

        private static final Board BOARD;
        static {
            Terrain[] fourStoresBuilding = new Terrain[] {
                  new Terrain(Terrains.BUILDING, 2), // building type medium
                  new Terrain(Terrains.BLDG_CF, 100), // building has CF 100
                  new Terrain(Terrains.BLDG_ELEV, 4),   // height of the building is 2
                  new Terrain(Terrains.BLDG_BASEMENT_TYPE, 2) // basement with depth 2
            };
            Terrain[] twoDepthWater = new Terrain[] {
                  new Terrain(Terrains.WATER, 2), // water with depth 2
            };
            Terrain[] ultraSubLevelWithBridge = new Terrain[] {
                  new Terrain(Terrains.BRIDGE, 0),
                  new Terrain(Terrains.BRIDGE_CF, 100),
                  new Terrain(Terrains.BRIDGE_ELEV, 2),
                  new Terrain(Terrains.ULTRA_SUBLEVEL, 0)
            };
            Terrain[] fourStoresHighBridge = new Terrain[] {
                  new Terrain(Terrains.BRIDGE, 0),
                  new Terrain(Terrains.BRIDGE_CF, 100),
                  new Terrain(Terrains.BRIDGE_ELEV, 4)
            };
            Terrain[] aboveWaterBridge = new Terrain[] {
                  new Terrain(Terrains.BRIDGE, 0),
                  new Terrain(Terrains.BRIDGE_CF, 100),
                  new Terrain(Terrains.BRIDGE_ELEV, 1),
                  new Terrain(Terrains.WATER, 2)
            };

            BOARD = new Board(1, 5);
            BOARD.setHex(0, 0, new Hex(0, fourStoresHighBridge, null));
            BOARD.setHex(0, 1, new Hex(0, fourStoresBuilding, null));
            BOARD.setHex(0, 2, new Hex(0, ultraSubLevelWithBridge, null));
            BOARD.setHex(0, 3, new Hex(0, twoDepthWater, null));
            BOARD.setHex(0, 4, new Hex(0, aboveWaterBridge, null));
        }

        private Game game;

        @BeforeEach
        void setUpEach() {
            game = new Game();
            game.setBoard(BOARD);
        }

        @Test
        void testJumpMovePath() {
            MovePath movePath = getMovePathFor(new BipedMek(), EntityMovementMode.BIPED);
            assertEquals(4, movePath.getStepVector().get(0).getElevation(),
                  "Jumping from the top of a bridge, level 0 with elevation 4");
            assertEquals(4, movePath.getStepVector().get(1).getElevation(),
                  "Passing over a building, level 0 with height 4");
            assertEquals(2, movePath.getStepVector().get(2).getElevation(),
                  "Jumping over a bridge, level 0 with elevation 2, on top of an ultra sublevel");
            assertEquals(-2, movePath.getStepVector().get(3).getElevation(),
                  "Jumping over a water hex, level 0 with depth 2");
            assertEquals(1, movePath.getStepVector().get(4).getElevation(),
                  "Jumping into a bridge, level 0 with elevation 1, on top of a water hex with depth 2");
        }

        @Test
        void wigeJumpMovePath() {
            MovePath movePath = getMovePathFor(new SupportTank(), EntityMovementMode.WIGE);
            assertEquals(5, movePath.getStepVector().get(0).getElevation(),
                  "Jumping from the top of a bridge, level 0 with elevation 4");
            assertEquals(5, movePath.getStepVector().get(1).getElevation(),
                  "Passing over a building, level 0 with height 4");
            assertEquals(3, movePath.getStepVector().get(2).getElevation(),
                  "Jumping over a bridge, level 0 with elevation 2, on top of an ultra sublevel");
            assertEquals(1, movePath.getStepVector().get(3).getElevation(),
                  "WiGE jumping over a water hex, level 0 with depth 2, should stay at elevation 1");
            assertEquals(2, movePath.getStepVector().get(4).getElevation(),
                  "Jumping into a bridge, level 0 with elevation 1, on top of a water hex with depth 2, wige stays at" +
                        " one elevation above the bridge");
        }

        @Test
        void testHoverJumpMovePath() {
            MovePath movePath = getMovePathFor(new SupportTank(), EntityMovementMode.HOVER);
            assertEquals(4, movePath.getStepVector().get(0).getElevation(),
                  "Jumping from the top of a bridge, level 0 with elevation 4");
            assertEquals(4, movePath.getStepVector().get(1).getElevation(),
                  "Passing over a building, level 0 with height 4");
            assertEquals(2, movePath.getStepVector().get(2).getElevation(),
                  "Jumping over a bridge, level 0 with elevation 2, on top of an ultra sublevel");
            assertEquals(0, movePath.getStepVector().get(3).getElevation(),
                  "Hover jumping over a water hex, level 0 with depth 2, should stay at elevation 0");
            assertEquals(1, movePath.getStepVector().get(4).getElevation(),
                  "Jumping into a bridge, level 0 with elevation 1, on top of a water hex with depth 2");
        }

        private MovePath getMovePathFor(Entity entity, EntityMovementMode movementMode) {
            return getMovePath(new MovePath(game, initializeUnit(entity, game, movementMode)));
        }
    }

    private static <T extends Entity> T initializeUnit(
          T unit,
          Game game,
          EntityMovementMode movementMode
    ) {
        return initializeUnit(unit, game, movementMode, Integer.MAX_VALUE);
    }

    private static <T extends Entity> T initializeUnit(
          T unit,
          Game game,
          EntityMovementMode movementMode,
          int startingElevation
    ) {
        if (movementMode != null) {
            unit.setMovementMode(movementMode);
        }
        if (unit instanceof Infantry) {
            unit.setWeight(5.0);
        } else {
            unit.setWeight(50.0);
        }
        unit.setOriginalWalkMP(8);
        unit.setOriginalJumpMP(8);
        unit.setId(5);
        game.addEntity(unit);
        unit.setPosition(new Coords(0, 0));
        unit.setFacing(3);

        if (startingElevation != Integer.MAX_VALUE) {
            unit.setElevation(startingElevation);
        } else {
            int elevationModifier = EntityMovementMode.WIGE.equals(movementMode) ? 1 : 0;
            unit.setElevation(game.getBoard().getHex(unit.getPosition()).ceiling() + elevationModifier);
        }

        return unit;
    }

    private record StepLog(MoveStep step) {
        @Override
        public String toString() {
            return new ToStringBuilder(this).append("step", step)
                         .append("entity", step.getEntity())
                         .append("mode", step.getMovementMode())
                         .append("distance", step.getDistance())
                         .append("elevation", step.getElevation())
                         .append("position", step.getPosition())
                         .toString();
        }
    }

    /**
     * Generates the movepath for the test
     * @param path the game
     * @return the MovePath
     */
    private static MovePath getMovePath(MovePath path) {
        return getMovePath(path,
              MovePath.MoveStepType.START_JUMP,
              MovePath.MoveStepType.FORWARDS,
              MovePath.MoveStepType.FORWARDS,
              MovePath.MoveStepType.FORWARDS,
              MovePath.MoveStepType.FORWARDS);
    }

    /**
     * Generates the movepath for the test
     * @param path the game
     * @return the MovePath
     */
    private static MovePath getMovePath(final MovePath path,final MovePath.MoveStepType... steps) {
        MovePath movePath = path.clone();
        for (var step : steps) {
            movePath = movePath.addStep(step);
        }
        return movePath;
    }

}

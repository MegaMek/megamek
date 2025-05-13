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
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
                  "WiGE jumping over a water hex, level 0 with depth 2, should stay at elevation 0");
            assertEquals(2, movePath.getStepVector().get(4).getElevation(),
                  "Jumping into a bridge, level 0 with elevation 1, on top of a water hex with depth 2");
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

    private static <T extends Entity> T initializeUnit(T unit, Game game, EntityMovementMode movementMode) {
        if (movementMode != null) {
            unit.setMovementMode(movementMode);
        }
        unit.setWeight(50.0);
        unit.setOriginalWalkMP(8);
        unit.setOriginalJumpMP(8);
        unit.setId(5);
        game.addEntity(unit);
        unit.setPosition(new Coords(0, 0));
        unit.setFacing(3);
        int elevation = EntityMovementMode.WIGE.equals(movementMode) ? 1 : 0;
        unit.setElevation(game.getBoard().getHex(unit.getPosition()).ceiling() + elevation);

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
        MovePath movePath = path.addStep(MovePath.MoveStepType.START_JUMP)
                                  .addStep(MovePath.MoveStepType.FORWARDS)
                                  .addStep(MovePath.MoveStepType.FORWARDS)
                                  .addStep(MovePath.MoveStepType.FORWARDS)
                                  .addStep(MovePath.MoveStepType.FORWARDS);
        movePath.getStepVector().stream().map(StepLog::new).forEach(System.out::println);
        return movePath;
    }
}

/*
 * Copyright (c) 2000-2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Vector;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import megamek.common.planetaryconditions.PlanetaryConditions;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 12/23/13 9:16 AM
 */
class MovePathTest {

    @BeforeAll
    static void setUp() {
        EquipmentType.initializeTypes();
    }

    @Test
    void testGetLastStep() {
        Game mockGame = mock(Game.class);
        PlanetaryConditions mockPC = new PlanetaryConditions();
        mockPC.setGravity(1.0f);
        when(mockGame.getPlanetaryConditions()).thenReturn(mockPC);

        Entity mockMek = mock(BipedMek.class);

        Vector<MoveStep> stepVector = new Vector<>();

        MoveStep mockStep1 = mock(MoveStep.class);
        stepVector.add(mockStep1);

        MoveStep mockStep2 = mock(MoveStep.class);
        stepVector.add(mockStep2);

        MoveStep mockStep3 = mock(MoveStep.class);
        stepVector.add(mockStep3);

        MoveStep mockStep4 = mock(MoveStep.class);
        stepVector.add(mockStep4);

        MovePath testPath = spy(new MovePath(mockGame, mockMek));
        doReturn(stepVector).when(testPath).getStepVector();

        assertEquals(mockStep4, testPath.getLastStep());

        stepVector.add(null);
        assertEquals(mockStep4, testPath.getLastStep());
    }

    @Nested
    class JumpingUnits {

        private static final Board BOARD_FOR_JUMPING;
        static {
            var building = new Terrain[] {
                  new Terrain(Terrains.BUILDING, 2),
                  new Terrain(Terrains.BLDG_CF, 100),
                  new Terrain(Terrains.BLDG_ELEV, 4),
                  new Terrain(Terrains.BLDG_BASEMENT_TYPE, 2)
            };
            BOARD_FOR_JUMPING = new Board(1, 4);
            BOARD_FOR_JUMPING.setHex(0, 0, new Hex(0));
            BOARD_FOR_JUMPING.setHex(0, 1, new Hex(0, building, null));
            BOARD_FOR_JUMPING.setHex(0, 2, new Hex(0, building, null));
            BOARD_FOR_JUMPING.setHex(0, 3, new Hex(0));
        }

        @Test
        void testJumpMovePath() {
            Game game = new Game();
            game.setBoard(BOARD_FOR_JUMPING);
            Entity mek = new BipedMek();
            mek.setOriginalWalkMP(8);
            mek.setOriginalJumpMP(8);
            mek.setId(5);
            game.addEntity(mek);

            mek.setPosition(new Coords(0, 0));
            mek.setFacing(3);

            MovePath movePath = new MovePath(game, mek).addStep(MovePath.MoveStepType.START_JUMP)
                                      .addStep(MovePath.MoveStepType.FORWARDS)
                                      .addStep(MovePath.MoveStepType.FORWARDS)
                                      .addStep(MovePath.MoveStepType.FORWARDS);

            movePath.getStepVector().stream().map(StepLog::new).forEach(System.out::println);

            assertEquals(0, movePath.getStepVector().get(0).getElevation());
            assertEquals(4, movePath.getStepVector().get(1).getElevation());
            assertEquals(4, movePath.getStepVector().get(2).getElevation());
            assertEquals(0, movePath.getStepVector().get(3).getElevation());
        }

        @Test
        void testHoverJumpMovePath() {
            Game game = new Game();
            game.setBoard(BOARD_FOR_JUMPING);

            SupportTank hover = new SupportTank();
            hover.setMovementMode(EntityMovementMode.HOVER);

            hover.setOriginalWalkMP(8);
            hover.setOriginalJumpMP(8);
            hover.setId(5);
            game.addEntity(hover);

            hover.setPosition(new Coords(0, 0));
            hover.setFacing(3);

            MovePath movePath = new MovePath(game, hover).addStep(MovePath.MoveStepType.START_JUMP)
                                      .addStep(MovePath.MoveStepType.FORWARDS)
                                      .addStep(MovePath.MoveStepType.FORWARDS)
                                      .addStep(MovePath.MoveStepType.FORWARDS);

            movePath.getStepVector().stream().map(StepLog::new).forEach(System.out::println);

            // This should be 0 - 4 - 8 - 8 in real time game
            assertEquals(0, movePath.getStepVector().get(0).getElevation());
            assertEquals(4, movePath.getStepVector().get(1).getElevation());
            assertEquals(8, movePath.getStepVector().get(2).getElevation());
            assertEquals(8, movePath.getStepVector().get(3).getElevation());
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
    }
}

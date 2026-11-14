/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version.
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
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by
 * or affiliated with Microsoft.
 */
package megamek.common.pathfinder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.MoveStepType;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import org.junit.jupiter.api.Test;

class MechanicalBoosterJumpPathFinderTest {

    @Test
    void boosterJumpExpandsInEveryDirectionWithoutTurningAtEveryFacing() {
        for (int facing = 0; facing < 6; facing++) {
            assertBoosterJumpExpandsInEveryDirectionWithoutTurning(facing);
        }
    }

    private void assertBoosterJumpExpandsInEveryDirectionWithoutTurning(int facing) {
        MovePath path = mock(MovePath.class);
        Game game = mock(Game.class);
        Board board = mock(Board.class);
        Coords origin = new Coords(5, 5);
        List<MoveStepType> addedSteps = new ArrayList<>();
        List<MovePath> extensions = new ArrayList<>();

        when(path.contains(MoveStepType.JUMP_MEK_MECHANICAL_BOOSTER)).thenReturn(true);
        when(path.getGame()).thenReturn(game);
        when(path.getFinalBoardId()).thenReturn(0);
        when(path.getFinalCoords()).thenReturn(origin);
        when(path.getFinalFacing()).thenReturn(facing);
        when(game.getBoard(0)).thenReturn(board);
        when(board.contains(any(Coords.class))).thenReturn(true);

        for (int direction = 0; direction < 6; direction++) {
            MovePath extension = mock(MovePath.class);
            extensions.add(extension);
            when(extension.addStep(any(MoveStepType.class))).thenAnswer(invocation -> {
                addedSteps.add(invocation.getArgument(0));
                return extension;
            });
        }
        when(path.clone()).thenReturn(extensions.get(0),
              extensions.subList(1, extensions.size()).toArray(MovePath[]::new));

        Collection<MovePath> adjacent = new NextStepsAdjacencyMap(MoveStepType.FORWARDS).getAdjacent(path);

        Set<MoveStepType> expectedSteps = EnumSet.noneOf(MoveStepType.class);
        for (int direction = 0; direction < 6; direction++) {
            expectedSteps.add(MoveStepType.stepTypeForRelativeDirection(direction, facing));
        }
        assertEquals(6, adjacent.size());
        assertEquals(expectedSteps, EnumSet.copyOf(addedSteps));
        assertTrue(addedSteps.stream()
                    .noneMatch(step -> step == MoveStepType.TURN_LEFT || step == MoveStepType.TURN_RIGHT));
    }

    @Test
    void backwardsBoosterJumpKeepsItsFacingInTheDestinationNode() {
        assertBackwardPathDestination(true, 4);
    }

    @Test
    void ordinaryBackwardMovementStillUsesItsOppositeFacingDestination() {
        assertBackwardPathDestination(false, 1);
    }

    private void assertBackwardPathDestination(boolean mechanicalBoosterJump, int expectedFacing) {
        MovePath path = mock(MovePath.class);
        MoveStep lastStep = mock(MoveStep.class);
        Coords destination = new Coords(7, 5);

        when(path.getLastStep()).thenReturn(lastStep);
        when(lastStep.isThisStepBackwards()).thenReturn(true);
        when(path.contains(MoveStepType.JUMP_MEK_MECHANICAL_BOOSTER)).thenReturn(mechanicalBoosterJump);
        when(path.getFinalCoords()).thenReturn(destination);
        when(path.getFinalFacing()).thenReturn(4);

        assertEquals(new CoordsWithFacing(destination, expectedFacing), new MovePathDestinationMap().getDestination(path));
    }
}

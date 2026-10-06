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

import java.util.List;

import megamek.common.GameBoardTestCase;
import megamek.common.board.Coords;
import megamek.common.enums.MoveStepType;
import megamek.common.units.QuadMek;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for issue #8446: a quad clicking one of its lateral-shift (sidestep) hexes must plot a lateral
 * shift into that hex, even when the hex costs extra movement points to enter (rough, rubble). A lateral shift and a
 * turn-then-forward pair reach the same side hex for the same MP; before the fix the pathfinder could resolve that tie
 * to the two-step turn-and-walk, which changed the quad's facing instead of sidestepping. The
 * {@link megamek.common.pathfinder.comparators.MovePathAStarComparator} now breaks such ties toward the path with
 * fewer steps, so the single-step lateral shift wins.
 */
class QuadLateralShiftPathTest extends GameBoardTestCase {

    static {
        initializeBoard("LATERAL_CLEAR_5x5", buildBoard(false));
        initializeBoard("LATERAL_ROUGH_5x5", buildBoard(true));
    }

    private static String buildBoard(boolean rough) {
        StringBuilder builder = new StringBuilder("size 5 5\n");
        for (int column = 1; column <= 5; column++) {
            for (int row = 1; row <= 5; row++) {
                String terrain = rough ? "rough:1" : "";
                builder.append(String.format("hex %02d%02d 0 \"%s\" \"\"%n", column, row, terrain));
            }
        }
        builder.append("end");
        return builder.toString();
    }

    /**
     * Adds a quad to the game and places it in the middle of the 5x5 board facing south, so that both of its
     * front-flanking lateral-shift hexes lie on the board.
     */
    private QuadMek centeredQuad(String board) {
        setBoard(board);
        QuadMek quad = new QuadMek();
        // getMovePathFor adds the quad to the game; re-centre it afterwards (the harness places units at 0,0).
        getMovePathFor(quad);
        quad.setPosition(new Coords(2, 2));
        quad.setFacing(3);
        return quad;
    }

    /**
     * Plots a path from the quad's starting hex to the hex a single lateral shift of the given type would reach, then
     * returns the step types of the resulting path.
     */
    private List<MoveStepType> pathToLateralHex(QuadMek quad, MoveStepType lateralShift) {
        // A single lateral shift tells us which side hex the pathfinder should reach.
        MovePath probe = new MovePath(getGame(), quad).addStep(lateralShift);
        Coords sideHex = probe.getFinalCoords();
        assertTrue(probe.isMoveLegal(), "single lateral shift should be legal on the test board");

        MovePath plotted = new MovePath(getGame(), quad);
        plotted.findPathTo(sideHex, MoveStepType.FORWARDS);
        assertTrue(plotted.isMoveLegal(), "plotted path to the side hex should be legal");
        assertEquals(sideHex, plotted.getFinalCoords(), "plotted path should reach the clicked side hex");
        return plotted.getStepVector().stream().map(MoveStep::getType).toList();
    }

    @Test
    @DisplayName("Quad sidesteps into a clear side hex with a single lateral-shift step")
    void quadSidestepsIntoClearHex() {
        QuadMek quad = centeredQuad("LATERAL_CLEAR_5x5");
        assertEquals(List.of(MoveStepType.LATERAL_LEFT), pathToLateralHex(quad, MoveStepType.LATERAL_LEFT));
        assertEquals(List.of(MoveStepType.LATERAL_RIGHT), pathToLateralHex(quad, MoveStepType.LATERAL_RIGHT));
    }

    @Test
    @DisplayName("Quad sidesteps into an extra-MP (rough) side hex instead of turning and walking (issue #8446)")
    void quadSidestepsIntoRoughHex() {
        QuadMek quad = centeredQuad("LATERAL_ROUGH_5x5");
        assertEquals(List.of(MoveStepType.LATERAL_LEFT), pathToLateralHex(quad, MoveStepType.LATERAL_LEFT),
              "quad should sidestep into rough, not turn and walk forward");
        assertEquals(List.of(MoveStepType.LATERAL_RIGHT), pathToLateralHex(quad, MoveStepType.LATERAL_RIGHT),
              "quad should sidestep into rough, not turn and walk forward");
    }
}

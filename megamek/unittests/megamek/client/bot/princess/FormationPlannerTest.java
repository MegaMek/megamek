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
package megamek.client.bot.princess;

import static org.junit.jupiter.api.Assertions.assertEquals;

import megamek.common.board.Coords;
import megamek.common.orders.FormationShape;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FormationPlanner}: where each shape puts each unit, heading north with spacing 2, and that the
 * shape turns with the heading.
 */
class FormationPlannerTest {

    private static final Coords LEADER = new Coords(10, 10);
    private static final int NORTH = 0;
    private static final int NORTH_EAST = 1;
    private static final int SOUTH_EAST = 2;
    private static final int SOUTH = 3;
    private static final int SOUTH_WEST = 4;
    private static final int NORTH_WEST = 5;
    private static final int SPACING = 2;

    private static Coords slot(FormationShape shape, int heading, int slotIndex) {
        return FormationPlanner.idealSlot(LEADER, heading, shape, SPACING, slotIndex);
    }

    @Test
    void anEchelonRightStepsBackAlongTheSouthEastLine() {
        assertEquals(LEADER.translated(SOUTH_EAST, 2), slot(FormationShape.ECHELON_RIGHT, NORTH, 1));
        assertEquals(LEADER.translated(SOUTH_EAST, 4), slot(FormationShape.ECHELON_RIGHT, NORTH, 2));
    }

    @Test
    void anEchelonLeftStepsBackAlongTheSouthWestLine() {
        assertEquals(LEADER.translated(SOUTH_WEST, 2), slot(FormationShape.ECHELON_LEFT, NORTH, 1));
    }

    @Test
    void aColumnFollowsStraightBehind() {
        assertEquals(LEADER.translated(SOUTH, 2), slot(FormationShape.COLUMN, NORTH, 1));
        assertEquals(LEADER.translated(SOUTH, 6), slot(FormationShape.COLUMN, NORTH, 3));
    }

    @Test
    void aWedgeStepsBackToBothSidesInTurn() {
        assertEquals(LEADER.translated(SOUTH_WEST, 2), slot(FormationShape.WEDGE, NORTH, 1));
        assertEquals(LEADER.translated(SOUTH_EAST, 2), slot(FormationShape.WEDGE, NORTH, 2));
        assertEquals(LEADER.translated(SOUTH_WEST, 4), slot(FormationShape.WEDGE, NORTH, 3));
    }

    @Test
    void aVeeStepsForwardToBothSides() {
        assertEquals(LEADER.translated(NORTH_WEST, 2), slot(FormationShape.VEE, NORTH, 1));
        assertEquals(LEADER.translated(NORTH_EAST, 2), slot(FormationShape.VEE, NORTH, 2));
    }

    @Test
    void aLineRunsAcrossTheHeadingInAZigzag() {
        // two steps to the right: north-east, then south-east, which comes back to the leader's row
        Coords rightOfLeader = slot(FormationShape.LINE, NORTH, 2);
        assertEquals(LEADER.translated(NORTH_EAST).translated(SOUTH_EAST), rightOfLeader);
        assertEquals(SPACING, LEADER.distance(rightOfLeader));
        assertEquals(LEADER.getY(), rightOfLeader.getY());
    }

    @Test
    void theShapeTurnsWithTheHeading() {
        // heading north-east, an Echelon Right steps back along the south line
        assertEquals(LEADER.translated(SOUTH, 2), slot(FormationShape.ECHELON_RIGHT, NORTH_EAST, 1));
    }
}

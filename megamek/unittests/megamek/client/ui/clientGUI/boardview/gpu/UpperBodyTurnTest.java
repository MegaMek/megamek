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
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UpperBodyTurnTest {
    private static final float TOLERANCE = 0.001f;

    @Test
    void aUnitFirstSeenTwistedDoesNotSwingIntoPlace() {
        UpperBodyTurn turn = new UpperBodyTurn();
        assertTrue(turn.advance(1, 0));
        assertEquals(60, turn.degrees(), TOLERANCE);
        assertFalse(turn.advance(1, 1));
    }

    @Test
    void aNewTwistSwingsOverAQuarterSecondPerHexside() {
        UpperBodyTurn turn = new UpperBodyTurn();
        turn.advance(0, 0);
        assertTrue(turn.advance(1, 0.125f));
        assertEquals(30, turn.degrees(), TOLERANCE);
        assertEquals(60, turn.targetDegrees(), TOLERANCE);
        assertTrue(turn.advance(1, 0.125f));
        assertEquals(60, turn.degrees(), TOLERANCE);
        assertFalse(turn.advance(1, 0.125f));
    }

    @Test
    void aLongFrameStopsAtTheTargetInsteadOfOvershooting() {
        UpperBodyTurn turn = new UpperBodyTurn();
        turn.advance(0, 0);
        turn.advance(-1, 5);
        assertEquals(-60, turn.degrees(), TOLERANCE);
    }

    @Test
    void theSwingTakesTheShortWayRound() {
        UpperBodyTurn turn = new UpperBodyTurn();
        turn.advance(3, 0);
        // From three hexsides clockwise to two anticlockwise is one hexside further clockwise, not five back.
        turn.advance(-2, 0.125f);
        assertEquals(210, turn.degrees(), TOLERANCE);
        turn.advance(-2, 1);
        assertEquals(0, UpperBodyTurn.shortestTurn(turn.degrees() - turn.targetDegrees()), TOLERANCE);
    }

    @Test
    void shortestTurnWrapsIntoHalfATurnEitherWay() {
        assertEquals(60, UpperBodyTurn.shortestTurn(-300), TOLERANCE);
        assertEquals(-60, UpperBodyTurn.shortestTurn(300), TOLERANCE);
        assertEquals(180, UpperBodyTurn.shortestTurn(180), TOLERANCE);
        assertEquals(180, UpperBodyTurn.shortestTurn(-180), TOLERANCE);
        assertEquals(0, UpperBodyTurn.shortestTurn(720), TOLERANCE);
    }
}

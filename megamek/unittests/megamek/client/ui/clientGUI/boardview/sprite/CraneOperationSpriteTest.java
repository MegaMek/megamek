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
package megamek.client.ui.clientGUI.boardview.sprite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.units.CraneOperation;
import megamek.common.units.Entity;
import megamek.common.units.VTOL;
import org.junit.jupiter.api.Test;

/**
 * Checks the opacity a unit moved by cranes is drawn with (TW p.90-91).
 */
class CraneOperationSpriteTest {

    private static final float TOLERANCE = 0.0001f;

    @Test
    void loadingFadesOutOneQuarterPerTurnOfFour() {
        // Loading passes the turns still to go: 4 left is fully visible, 1 left is a quarter
        assertEquals(1f, CraneOperationSprite.fadeOpacity(4, 4), TOLERANCE);
        assertEquals(0.75f, CraneOperationSprite.fadeOpacity(3, 4), TOLERANCE);
        assertEquals(0.5f, CraneOperationSprite.fadeOpacity(2, 4), TOLERANCE);
        assertEquals(0.25f, CraneOperationSprite.fadeOpacity(1, 4), TOLERANCE);
    }

    @Test
    void unloadingFadesInOneThirdPerTurnOfThree() {
        // Unloading passes the turns banked: 2 of 3 is two thirds, 3 of 3 is fully visible
        assertEquals(1f / 3f, CraneOperationSprite.fadeOpacity(1, 3), TOLERANCE);
        assertEquals(2f / 3f, CraneOperationSprite.fadeOpacity(2, 3), TOLERANCE);
        assertEquals(1f, CraneOperationSprite.fadeOpacity(3, 3), TOLERANCE);
    }

    @Test
    void neverFadesBelowTheFaintMinimum() {
        assertEquals(0.18f, CraneOperationSprite.fadeOpacity(0, 3), TOLERANCE);
        assertEquals(0.18f, CraneOperationSprite.fadeOpacity(0, 0), TOLERANCE);
    }

    @Test
    void unitThatMovesIsNoLongerWaitingInPlace() {
        Coords waitingHex = new Coords(3, 1);
        VTOL vtol = new VTOL();
        vtol.setId(30);
        vtol.setPosition(waitingHex);
        CraneOperation loading = CraneOperation.load(vtol.getId(), waitingHex);
        assertTrue(CraneOperationSprite.isWaitingInPlace(vtol, loading), "Still in the hex it declared from");

        vtol.delta_distance = 1;
        assertFalse(CraneOperationSprite.isWaitingInPlace(vtol, loading), "Moved this turn, even if it came back");

        vtol.delta_distance = 2;
        vtol.setPosition(new Coords(1, 1));
        assertFalse(CraneOperationSprite.isWaitingInPlace(vtol, loading), "Left the hex it declared from");
    }

    @Test
    void unitNoCraneCanMoveIsFullyVisible() {
        // A plain unit is not a crane-only unit, so no carrier is even looked for
        Entity mek = mock(Entity.class);
        assertEquals(1f, CraneOperationSprite.loadingOpacity(mek, mock(Game.class)), TOLERANCE);
    }
}

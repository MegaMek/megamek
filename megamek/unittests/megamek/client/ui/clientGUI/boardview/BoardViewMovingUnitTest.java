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

package megamek.client.ui.clientGUI.boardview;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Vector;

import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.units.Tank;
import megamek.common.units.UnitLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Regression test for megamek #8973: the move animation must not be started for a unit that has no position after
 * its move (it mounted a DropShip, was recovered by a carrier, or left the board).
 */
class BoardViewMovingUnitTest {

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @Test
    @DisplayName("A unit with no position after its move is not animated")
    void unitWithoutPositionIsNotAnimated() {
        Tank tank = new Tank();
        tank.setId(1);
        // The load cleared the position; the entity update still carries the path the unit walked
        tank.setPosition(null);

        assertFalse(BoardView.canAnimateMove(tank), "A unit with no position has no hex to draw a ghost in");
        assertFalse(BoardView.canAnimateMove(null), "A missing unit is never animated");
    }

    @Test
    @DisplayName("A unit that still has a position is animated as before")
    void unitWithPositionIsAnimated() {
        Tank tank = new Tank();
        tank.setId(1);
        tank.setPosition(new Coords(2, 2));

        assertTrue(BoardView.canAnimateMove(tank), "An ordinary move is still animated");
    }

    /**
     * Drives the guard through the real {@link BoardView}, which is what threw in the issue. Constructing a BoardView
     * reads the tileset from the shipped data set, which the test task does not stage, so this is tagged
     * {@code on-demand} and excluded from the normal test run. Run it with
     * {@code gradlew :megamek:test --tests BoardViewMovingUnitTest -PincludeTags=on-demand} on a staged checkout.
     */
    @Test
    @Tag("on-demand")
    @DisplayName("addMovingUnit skips a unit with no position (needs staged data)")
    void addMovingUnitSkipsUnitWithoutPosition() throws Exception {
        Game game = new Game();
        BoardView boardView = new BoardView(game, null, null, 0);

        Tank tank = new Tank();
        tank.setId(1);
        tank.setGame(game);
        tank.setPosition(null);
        Vector<UnitLocation> movePath = new Vector<>();
        movePath.add(new UnitLocation(tank.getId(), new Coords(2, 2), 0, 0, 0));

        assertDoesNotThrow(() -> boardView.addMovingUnit(tank, movePath));
        assertFalse(boardView.isMovingUnits());
    }
}

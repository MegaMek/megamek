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

import java.util.Vector;

import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.units.Tank;
import megamek.common.units.UnitLocation;
import org.junit.jupiter.api.BeforeAll;
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
    void addMovingUnitSkipsUnitWithoutPosition() throws Exception {
        Game game = new Game();
        BoardView boardView = new BoardView(game, null, null, 0);

        Tank tank = new Tank();
        tank.setId(1);
        tank.setGame(game);
        // The mount cleared the position; the entity update still carries the path it walked
        tank.setPosition(null);
        Vector<UnitLocation> movePath = new Vector<>();
        movePath.add(new UnitLocation(tank.getId(), new Coords(2, 2), 0, 0, 0));

        assertDoesNotThrow(() -> boardView.addMovingUnit(tank, movePath));
        assertFalse(boardView.isMovingUnits());
    }
}

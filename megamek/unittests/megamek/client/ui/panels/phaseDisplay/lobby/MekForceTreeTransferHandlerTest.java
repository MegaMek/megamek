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
package megamek.client.ui.panels.phaseDisplay.lobby;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.List;

import megamek.common.Player;
import megamek.common.force.Force;
import megamek.common.icons.Camouflage;
import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;

/**
 * The index maths behind dragging a force or unit between two rows of the lobby force tree: where a drop point lands
 * in the dragged item's own list.
 */
class MekForceTreeTransferHandlerTest {

    private static final Player PLAYER = new Player(0, "Player");
    private static final Player ALLY = new Player(1, "Ally");

    private static Force topLevel(String name, int id, Player owner) {
        return new Force(name, id, new Camouflage(), owner);
    }

    @Test
    void movingDownCountsTheItemItself() {
        // A, B, C with A dragged to the gap below B (insertion point 2): A lands in place 1
        assertEquals(1, MekForceTreeTransferHandler.placeAfterMove(2, 0));
    }

    @Test
    void movingUpKeepsTheInsertionPoint() {
        // A, B, C with C dragged to the gap above A (insertion point 0): C lands in place 0
        assertEquals(0, MekForceTreeTransferHandler.placeAfterMove(0, 2));
    }

    @Test
    void droppingRightAboveOrBelowItselfIsNoMove() {
        assertEquals(1, MekForceTreeTransferHandler.placeAfterMove(1, 1));
        assertEquals(1, MekForceTreeTransferHandler.placeAfterMove(2, 1));
    }

    @Test
    void topLevelDropCountsOnlyTheOwnersOtherForces() {
        Force alpha = topLevel("Alpha", 0, PLAYER);
        Force bravo = topLevel("Bravo", 1, PLAYER);
        Force charlie = topLevel("Charlie", 2, PLAYER);
        Force allyForce = topLevel("Ally Lance", 3, ALLY);
        Entity looseUnit = mock(Entity.class);
        // The tree lists my forces, then an ally's, then units outside any force
        List<Object> shown = List.of(alpha, bravo, charlie, allyForce, looseUnit);

        // Charlie to the gap between Alpha and Bravo
        assertEquals(1, MekForceTreeTransferHandler.topLevelTargetIndex(shown, charlie, 1));
        // Alpha to the gap below Charlie
        assertEquals(2, MekForceTreeTransferHandler.topLevelTargetIndex(shown, alpha, 3));
        // Alpha dropped among the ally's forces or the loose units lands at the end of my forces
        assertEquals(2, MekForceTreeTransferHandler.topLevelTargetIndex(shown, alpha, 4));
        assertEquals(2, MekForceTreeTransferHandler.topLevelTargetIndex(shown, alpha, 5));
        // Bravo to the top
        assertEquals(0, MekForceTreeTransferHandler.topLevelTargetIndex(shown, bravo, 0));
    }
}

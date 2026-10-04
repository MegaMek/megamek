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
import static org.junit.jupiter.api.Assertions.assertFalse;

import megamek.common.GameBoardTestCase;
import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.enums.MoveStepType;
import megamek.common.units.BipedMek;
import megamek.common.units.Dropship;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A Mek may move through a hex holding a friendly Mek but may not end its move there (TW, Occupied Hexes and
 * Stacking). Mounting an adjacent DropShip from that hex does not end the move in the hex, so the path must survive
 * {@link MovePath#clipToPossible()}. Regression test for issue #8589.
 */
class MountThroughFriendlyMekTest extends GameBoardTestCase {

    private static final Coords FRIENDLY_MEK_HEX = new Coords(0, 1);
    private static final Coords DROPSHIP_HEX = new Coords(0, 2);

    static {
        initializeBoard("COLUMN_BOARD", """
              size 1 3
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 0 "" ""
              end""");
    }

    @Test
    @DisplayName("A Mek can walk into a friendly Mek's hex and mount the adjacent DropShip")
    void mekMountsDropShipFromFriendlyMekHex() {
        MovePath movePath = walkIntoFriendlyMekHex();
        movePath.addStep(MoveStepType.MOUNT, createDropShip());

        movePath.clipToPossible();

        assertEquals(2, movePath.length(), "Both the walk step and the mount step should survive clipping");
        assertEquals(FRIENDLY_MEK_HEX, movePath.getStep(0).getPosition(),
              "The walk step should still enter the friendly Mek's hex");
        assertEquals(MoveStepType.MOUNT, movePath.getLastStep().getType(), "The path should still end by mounting");
    }

    @Test
    @DisplayName("A Mek still cannot end its move in a friendly Mek's hex without mounting")
    void mekCannotEndMoveInFriendlyMekHex() {
        MovePath movePath = walkIntoFriendlyMekHex();

        assertFalse(movePath.getLastStep().isLegalEndPos(),
              "A friendly Mek in the hex should make it an illegal place to end the move");

        movePath.clipToPossible();

        assertEquals(0, movePath.length(), "Ending the move in the friendly Mek's hex should be clipped away");
    }

    /**
     * Builds a Mek path that walks one hex south, into a hex already holding a deployed friendly Mek.
     */
    private MovePath walkIntoFriendlyMekHex() {
        setBoard("COLUMN_BOARD");
        Player player = new Player(0, "Player");
        getGame().addPlayer(player.getId(), player);

        BipedMek friendlyMek = new BipedMek();
        friendlyMek.setId(10);
        friendlyMek.setOwner(player);
        getGame().addEntity(friendlyMek);
        friendlyMek.setDeployed(true);
        friendlyMek.setElevation(0);
        friendlyMek.setPosition(FRIENDLY_MEK_HEX);

        BipedMek movingMek = new BipedMek();
        movingMek.setOwner(player);
        movingMek.setDeployed(true);
        return getMovePathFor(movingMek, MoveStepType.FORWARDS);
    }

    private Dropship createDropShip() {
        Dropship dropShip = new Dropship();
        dropShip.setId(20);
        dropShip.setPosition(DROPSHIP_HEX);
        return dropShip;
    }
}

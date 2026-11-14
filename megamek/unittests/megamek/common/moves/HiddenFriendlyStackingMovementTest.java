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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.GameBoardTestCase;
import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.enums.MoveStepType;
import megamek.common.units.BipedMek;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A Mek may not end its move in a hex holding a friendly Mek, and that stays true when the friendly Mek is hidden: its
 * own side knows it is there. A hidden enemy is different. The mover does not know about it, so the hex must stay a
 * legal place to end the move, and the server then stops the mover and reveals the hidden unit.
 */
class HiddenFriendlyStackingMovementTest extends GameBoardTestCase {

    private static final Coords HIDDEN_UNIT_HEX = new Coords(0, 1);
    private static final int MOVING_TEAM = 1;
    private static final int OTHER_TEAM = 2;

    static {
        initializeBoard("HIDDEN_STACKING_COLUMN", """
              size 1 3
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 0 "" ""
              end""");
    }

    @Test
    @DisplayName("A Mek cannot end its move on its own hidden Mek")
    void mekCannotEndMoveOnOwnHiddenMek() {
        Player owner = addPlayer(0, MOVING_TEAM);
        addHiddenMek(owner);

        MovePath movePath = getMovePathFor(newMovingMek(owner), MoveStepType.FORWARDS);

        assertFalse(movePath.getLastStep().isLegalEndPos(),
              "The player's own hidden Mek should make the hex an illegal place to end the move");
        assertFalse(movePath.isMoveLegal(), "A move ending on the player's own hidden Mek should be illegal");
    }

    @Test
    @DisplayName("A Mek cannot end its move on a teammate's hidden Mek")
    void mekCannotEndMoveOnHiddenTeammate() {
        Player owner = addPlayer(0, MOVING_TEAM);
        Player teammate = addPlayer(1, MOVING_TEAM);
        addHiddenMek(teammate);

        MovePath movePath = getMovePathFor(newMovingMek(owner), MoveStepType.FORWARDS);

        assertFalse(movePath.isMoveLegal(), "A move ending on a teammate's hidden Mek should be illegal");
    }

    @Test
    @DisplayName("A Mek can still walk through a hidden friendly Mek's hex")
    void mekCanWalkThroughHiddenFriendlyMek() {
        Player owner = addPlayer(0, MOVING_TEAM);
        addHiddenMek(owner);

        MovePath movePath = getMovePathFor(newMovingMek(owner), MoveStepType.FORWARDS, MoveStepType.FORWARDS);

        assertTrue(movePath.isMoveLegal(), "Passing through a hidden friendly Mek's hex should stay legal");
    }

    @Test
    @DisplayName("A hidden enemy Mek does not make its hex an illegal place to end a move")
    void hiddenEnemyMekDoesNotBlockTheMove() {
        Player owner = addPlayer(0, MOVING_TEAM);
        Player enemy = addPlayer(1, OTHER_TEAM);
        addHiddenMek(enemy);

        MovePath movePath = getMovePathFor(newMovingMek(owner), MoveStepType.FORWARDS);

        assertTrue(movePath.isMoveLegal(),
              "The mover does not know about a hidden enemy, so the move must compile as legal");
    }

    private Player addPlayer(int playerId, int team) {
        Player player = new Player(playerId, "Player " + playerId);
        player.setTeam(team);
        getGame().addPlayer(playerId, player);
        return player;
    }

    private void addHiddenMek(Player owner) {
        setBoard("HIDDEN_STACKING_COLUMN");
        BipedMek hiddenMek = new BipedMek();
        hiddenMek.setId(10);
        hiddenMek.setOwner(owner);
        getGame().addEntity(hiddenMek);
        hiddenMek.setDeployed(true);
        hiddenMek.setElevation(0);
        hiddenMek.setPosition(HIDDEN_UNIT_HEX);
        hiddenMek.setHidden(true);
    }

    private BipedMek newMovingMek(Player owner) {
        BipedMek movingMek = new BipedMek();
        movingMek.setOwner(owner);
        movingMek.setDeployed(true);
        return movingMek;
    }
}

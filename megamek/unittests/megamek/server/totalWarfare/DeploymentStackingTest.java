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

package megamek.server.totalWarfare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import megamek.common.GameBoardTestCase;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.game.Game;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Tank;
import megamek.server.GameManagerPacketHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the server's stacking check on deployment. Under double blind the client cannot see enemy units, so the server
 * must refuse a deployment onto one (issue 9488: a DropShip deployed over a hidden-from-view enemy tank could never
 * act again).
 */
class DeploymentStackingTest extends GameBoardTestCase {

    private static final String BOARD_NAME = "EMPTY_7x7";
    private static final Coords CENTRE = new Coords(3, 3);
    private static final int PLAYER_ID = 0;
    private static final int ENEMY_ID = 1;

    private Game game;
    private TWGameManager gameManager;
    private DeploymentServerHelper helper;
    private int nextEntityId = 1;

    static {
        StringBuilder boardText = new StringBuilder("size 7 7\n");
        for (int y = 1; y <= 7; y++) {
            for (int x = 1; x <= 7; x++) {
                boardText.append(String.format("hex %02d%02d 0 \"\" \"\"%n", x, y));
            }
        }
        boardText.append("end");
        initializeBoard(BOARD_NAME, boardText.toString());
    }

    @BeforeEach
    void beforeEach() {
        game = new Game();
        Board board = getBoard(BOARD_NAME);
        game.setBoard(0, board);
        game.addPlayer(PLAYER_ID, new Player(PLAYER_ID, "Player"));
        Player enemy = new Player(ENEMY_ID, "Enemy");
        enemy.setTeam(2);
        game.addPlayer(ENEMY_ID, enemy);
        game.setPhase(GamePhase.DEPLOYMENT);

        gameManager = mock(TWGameManager.class);
        when(gameManager.getGame()).thenReturn(game);
        when(gameManager.getPacketHelper()).thenReturn(mock(GameManagerPacketHelper.class));
        helper = new DeploymentServerHelper(gameManager);
    }

    private Tank deployedEnemyTank(Coords position) {
        Tank tank = new Tank();
        tank.setChassis("Enemy Tank");
        tank.setMovementMode(EntityMovementMode.TRACKED);
        tank.setWeight(60);
        tank.setId(nextEntityId++);
        tank.setOwner(game.getPlayer(ENEMY_ID));
        game.addEntity(tank);
        tank.setPosition(position);
        tank.setBoardId(0);
        tank.setDeployed(true);
        return tank;
    }

    private Dropship undeployedDropShip() {
        Dropship dropShip = new Dropship();
        dropShip.setChassis("Test DropShip");
        dropShip.setSpheroid(true);
        dropShip.setId(nextEntityId++);
        dropShip.setOwner(game.getPlayer(PLAYER_ID));
        game.addEntity(dropShip);
        return dropShip;
    }

    @Test
    void dropShipLandingOnEnemyInItsFootprintIsRefused() {
        // The Fortress in the issue was centred one hex from the enemy Manticore
        Tank enemyTank = deployedEnemyTank(CENTRE.translated(1));
        Dropship dropShip = undeployedDropShip();

        Entity blocker = helper.findDeploymentStackingViolation(dropShip, CENTRE, 0, 0, false);

        assertEquals(enemyTank, blocker, "an enemy in any of the DropShip's seven hexes blocks the landing");
    }

    @Test
    void dropShipLandingOnEnemyInItsCentreHexIsRefused() {
        Tank enemyTank = deployedEnemyTank(CENTRE);
        Dropship dropShip = undeployedDropShip();

        assertEquals(enemyTank, helper.findDeploymentStackingViolation(dropShip, CENTRE, 0, 0, false));
    }

    @Test
    void dropShipLandingNextToItsFootprintIsAllowed() {
        deployedEnemyTank(CENTRE.translated(1, 2));
        Dropship dropShip = undeployedDropShip();

        assertNull(helper.findDeploymentStackingViolation(dropShip, CENTRE, 0, 0, false),
              "a unit two hexes from the centre is outside the footprint");
    }

    @Test
    void hiddenEnemyStillBlocksTheLanding() {
        Tank enemyTank = deployedEnemyTank(CENTRE.translated(4));
        enemyTank.setHidden(true);
        Dropship dropShip = undeployedDropShip();

        assertEquals(enemyTank, helper.findDeploymentStackingViolation(dropShip, CENTRE, 0, 0, false),
              "a hidden unit is still physically in the hex");
    }

    @Test
    void dropShipDeployingInFlightIsAllowed() {
        deployedEnemyTank(CENTRE);
        Dropship dropShip = undeployedDropShip();

        assertNull(helper.findDeploymentStackingViolation(dropShip, CENTRE, 0, 3, false),
              "aerospace in flight never stacks with ground units");
    }

    @Test
    void assaultDropIsLeftToTheLanding() {
        deployedEnemyTank(CENTRE);
        Dropship dropShip = undeployedDropShip();

        assertNull(helper.findDeploymentStackingViolation(dropShip, CENTRE, 0, 0, true));
    }

    @Test
    void refusalTellsThePlayerAndSendsTheBlocker() {
        Tank enemyTank = deployedEnemyTank(CENTRE.translated(1));
        Dropship dropShip = undeployedDropShip();

        helper.refuseStackedDeployment(PLAYER_ID, dropShip, enemyTank, CENTRE, PLAYER_ID);

        verify(gameManager).createEntityPacket(enemyTank.getId(), null);
        verify(gameManager).createEntityPacket(dropShip.getId(), null);
        verify(gameManager).sendServerChat(eq(PLAYER_ID), anyString());
    }
}

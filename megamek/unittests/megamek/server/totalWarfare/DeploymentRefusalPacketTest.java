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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import megamek.common.GameBoardTestCase;
import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.game.Game;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.turns.SpecificEntityTurn;
import megamek.common.units.BipedMek;
import megamek.common.units.Mek;
import megamek.server.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * Sends a real deployment packet at the server, so the refusal is tested the way a client reaches it (issue 9580).
 * When the hex holds a unit the deploying player cannot see, the server must refuse, hand the turn back, and send
 * nothing that gives the unseen unit away.
 */
class DeploymentRefusalPacketTest extends GameBoardTestCase {

    private static final String BOARD_NAME = "EMPTY_5x5";
    private static final Coords TARGET_HEX = new Coords(2, 2);
    private static final int PLAYER_ID = 0;
    private static final int ENEMY_ID = 1;
    private static final int DEPLOYING_MEK_ID = 10;
    private static final int BLOCKING_MEK_ID = 11;

    private MockedStatic<Server> mockedServer;
    private Server server;
    private TWGameManager gameManager;
    private Game game;
    private Mek deployingMek;
    private Mek blockingMek;

    static {
        StringBuilder boardText = new StringBuilder("size 5 5\n");
        for (int y = 1; y <= 5; y++) {
            for (int x = 1; x <= 5; x++) {
                boardText.append(String.format("hex %02d%02d 0 \"\" \"\"%n", x, y));
            }
        }
        boardText.append("end");
        initializeBoard(BOARD_NAME, boardText.toString());
    }

    @BeforeEach
    void beforeEach() {
        // every packet leaves through the server, whichever helper class sends it, so it is recorded there
        server = mock(Server.class);
        mockedServer = mockStatic(Server.class);
        mockedServer.when(Server::getServerInstance).thenReturn(server);

        gameManager = new TWGameManager();
        game = gameManager.getGame();
        game.setBoard(0, getBoard(BOARD_NAME));
        Player player = new Player(PLAYER_ID, "Player");
        player.setTeam(1);
        game.addPlayer(PLAYER_ID, player);
        Player enemy = new Player(ENEMY_ID, "Enemy");
        enemy.setTeam(2);
        game.addPlayer(ENEMY_ID, enemy);
        game.setPhase(GamePhase.DEPLOYMENT);

        blockingMek = addMek(BLOCKING_MEK_ID, ENEMY_ID, "Blocker");
        blockingMek.setPosition(TARGET_HEX);
        blockingMek.setDeployed(true);
        deployingMek = addMek(DEPLOYING_MEK_ID, PLAYER_ID, "Deployer");

        game.setTurnVector(List.of(new SpecificEntityTurn(PLAYER_ID, DEPLOYING_MEK_ID)));
        game.setTurnIndex(0, PLAYER_ID);
    }

    @AfterEach
    void afterEach() {
        mockedServer.close();
    }

    private Mek addMek(int entityId, int ownerId, String chassis) {
        Mek mek = new BipedMek();
        mek.setChassis(chassis);
        mek.setId(entityId);
        mek.setOwner(game.getPlayer(ownerId));
        game.addEntity(mek);
        return mek;
    }

    /** Sends the deployment the way a client does, and returns every packet the server sent that player back. */
    private List<Packet> deployOntoTheBlocker() {
        gameManager.handlePacket(PLAYER_ID,
              new Packet(PacketCommand.ENTITY_DEPLOY, DEPLOYING_MEK_ID, TARGET_HEX, 0, 0, 0, 0, false));
        ArgumentCaptor<Packet> sentPackets = ArgumentCaptor.forClass(Packet.class);
        verify(server, atLeast(0)).send(eq(PLAYER_ID), sentPackets.capture());
        return sentPackets.getAllValues();
    }

    private static List<Integer> unitIdsSentIn(List<Packet> packets) throws Exception {
        List<Integer> unitIds = new ArrayList<>();
        for (Packet packet : packets) {
            if (packet.command() == PacketCommand.ENTITY_UPDATE) {
                unitIds.add(packet.getIntValue(0));
            }
        }
        return unitIds;
    }

    private static boolean handsTheTurnBack(List<Packet> packets) {
        boolean sawTurnPacket = false;
        for (Packet packet : packets) {
            if (packet.command() == PacketCommand.TURN) {
                sawTurnPacket = true;
            }
        }
        return sawTurnPacket;
    }

    @Test
    void aDeploymentOntoAHiddenEnemyIsRefusedWithoutSendingTheEnemy() throws Exception {
        blockingMek.setHidden(true);

        List<Packet> packets = deployOntoTheBlocker();

        assertFalse(deployingMek.isDeployed(), "the server must refuse to stack on the hidden unit");
        assertTrue(handsTheTurnBack(packets), "the turn is handed back so the player can pick another hex");
        assertEquals(List.of(DEPLOYING_MEK_ID), unitIdsSentIn(packets),
              "only the refused unit is resent; the hidden enemy must not reach the player");
    }

    @Test
    void aDeploymentOntoAnEnemyInPlainViewIsRefusedAndTheEnemyIsSent() throws Exception {
        List<Packet> packets = deployOntoTheBlocker();

        assertFalse(deployingMek.isDeployed());
        assertTrue(handsTheTurnBack(packets));
        assertEquals(List.of(BLOCKING_MEK_ID, DEPLOYING_MEK_ID), unitIdsSentIn(packets),
              "control: a blocker the player can see is still sent to them");
    }
}

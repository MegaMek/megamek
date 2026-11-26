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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.concurrent.TimeUnit;

import megamek.common.GameBoardTestCase;
import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.enums.MoveStepType;
import megamek.common.moves.MovePath;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.turns.SpecificEntityTurn;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Mek;
import megamek.server.Server;
import megamek.utils.ServerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * A move that ends on a hidden friendly Mek must not be carried out in full. With team vision off the mover's player
 * is never sent a teammate's hidden unit, so their client offers the hex and only the server can catch it. The server
 * stops the unit in the last hex it legally reached and tells the player the hex is not available, without naming the
 * hidden unit. The moves here are sent as real movement packets.
 */
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class HiddenFriendlyStackingServerTest extends GameBoardTestCase {

    private static final int MOVER_PLAYER_ID = 0;
    private static final int TEAMMATE_PLAYER_ID = 1;
    private static final int TEAM = 1;
    private static final int TEAMMATE_MEK_ID = 10;
    private static final Coords START_HEX = new Coords(0, 0);
    private static final Coords TEAMMATE_HEX = new Coords(0, 1);
    private static final Coords FAR_HEX = new Coords(0, 2);

    private Server server;
    private TWGameManager gameManager;
    private Player moverPlayer;
    private Mek teammateMek;

    static {
        initializeBoard("HIDDEN_STACKING_SERVER_COLUMN", """
              size 1 3
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 0 "" ""
              end""");
    }

    @BeforeEach
    void setUp() throws Exception {
        setBoard("HIDDEN_STACKING_SERVER_COLUMN");
        gameManager = spy(new TWGameManager());
        gameManager.setGame(getGame());
        server = ServerFactory.createServer(gameManager);

        moverPlayer = new Player(MOVER_PLAYER_ID, "Mover");
        moverPlayer.setTeam(TEAM);
        getGame().addPlayer(MOVER_PLAYER_ID, moverPlayer);
        Player teammate = new Player(TEAMMATE_PLAYER_ID, "Teammate");
        teammate.setTeam(TEAM);
        getGame().addPlayer(TEAMMATE_PLAYER_ID, teammate);

        teammateMek = newMek(teammate);
        teammateMek.setId(TEAMMATE_MEK_ID);
        getGame().addEntity(teammateMek);
        teammateMek.setDeployed(true);
        teammateMek.setElevation(0);
        teammateMek.setPosition(TEAMMATE_HEX);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.die();
        }
    }

    @Test
    void aMoveEndingOnAHiddenTeammateStopsOneHexShort() throws Exception {
        teammateMek.setHidden(true);
        Mek mover = newMek(moverPlayer);

        sendMove(mover, MoveStepType.FORWARDS);

        assertEquals(START_HEX, mover.getPosition(), "The mover should stop in the last hex it legally reached");
        assertEquals(TEAMMATE_HEX, teammateMek.getPosition(), "The hidden teammate should not be displaced");
        assertTrue(teammateMek.isHidden(), "The hidden teammate should stay hidden");
        verify(gameManager).sendServerChat(eq(MOVER_PLAYER_ID), anyString());
    }

    @Test
    void aMoveThroughAHiddenTeammateIsCarriedOutInFull() throws Exception {
        teammateMek.setHidden(true);
        Mek mover = newMek(moverPlayer);

        sendMove(mover, MoveStepType.FORWARDS, MoveStepType.FORWARDS);

        assertEquals(FAR_HEX, mover.getPosition(), "Passing through the hidden teammate's hex should stay legal");
        verify(gameManager, never()).sendServerChat(anyInt(), anyString());
    }

    @Test
    void aMoveEndingOnAVisibleTeammateSendsNoHiddenUnitMessage() throws Exception {
        Mek mover = newMek(moverPlayer);

        sendMove(mover, MoveStepType.FORWARDS);

        assertEquals(START_HEX, mover.getPosition(), "A move ending on a visible teammate was already stopped");
        verify(gameManager, never()).sendServerChat(anyInt(), anyString());
    }

    private void sendMove(Mek mover, MoveStepType... steps) throws Exception {
        MovePath movePath = getMovePathFor(mover, steps);
        getGame().setPhase(GamePhase.MOVEMENT);
        getGame().setTurnVector(List.of(new SpecificEntityTurn(MOVER_PLAYER_ID, mover.getId())));
        getGame().setTurnIndex(0, MOVER_PLAYER_ID);

        gameManager.handlePacket(MOVER_PLAYER_ID, new Packet(PacketCommand.ENTITY_MOVE, mover.getId(), movePath));
    }

    private Mek newMek(Player owner) {
        BipedMek mek = new BipedMek();
        mek.setChassis("Test");
        mek.setModel("Mek");
        mek.setWeight(50.0);
        mek.setOriginalWalkMP(8);
        mek.autoSetInternal();
        for (int location = 0; location < mek.locations(); location++) {
            mek.initializeArmor(10, location);
        }
        mek.setCrew(new Crew(CrewType.SINGLE));
        mek.setOwner(owner);
        mek.setDeployed(true);
        return mek;
    }
}

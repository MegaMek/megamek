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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.game.Game;
import megamek.common.game.InitiativeRoll;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Tests that a client joining mid-round is sent the team initiative rolls, ahead of the phase change. A bot decides on
 * a Tactical Genius reroll when it sees the Initiative Report phase begin, so a bot that connects during that phase
 * would otherwise judge without any team roll (issue #9275).
 */
class TeamInitiativeOnConnectTest {

    private static final int JOINING_CONNECTION = 0;
    private static final int OTHER_CONNECTION = 1;

    private TWGameManager gameManager;
    private Game game;
    private final List<Packet> packetsToJoiningClient = new ArrayList<>();

    @BeforeEach
    void setUp() {
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doAnswer(invocation -> {
            if ((int) invocation.getArgument(0) == JOINING_CONNECTION) {
                packetsToJoiningClient.add(invocation.getArgument(1));
            }
            return null;
        }).when(gameManager).send(anyInt(), any(Packet.class));
        Mockito.doNothing().when(gameManager).sendServerChat(anyInt(), anyString());
        // sendCurrentInfo moves a phase without turns along, which needs a live server; only the packets matter here
        Mockito.doNothing().when(gameManager).endCurrentPhase();
        game = gameManager.getGame();
        Player joining = new Player(JOINING_CONNECTION, "Princess");
        joining.setTeam(1);
        Player other = new Player(OTHER_CONNECTION, "Human");
        other.setTeam(2);
        game.addPlayer(JOINING_CONNECTION, joining);
        game.addPlayer(OTHER_CONNECTION, other);
        game.getTeamForPlayer(other).getInitiative().addRoll(4, "");
    }

    private int indexOf(PacketCommand command) {
        for (int i = 0; i < packetsToJoiningClient.size(); i++) {
            if (packetsToJoiningClient.get(i).command() == command) {
                return i;
            }
        }
        return -1;
    }

    @Test
    void aClientJoiningTheInitiativeReportGetsTheTeamRollsBeforeThePhaseChange() {
        game.setPhase(GamePhase.INITIATIVE_REPORT);

        gameManager.sendCurrentInfo(JOINING_CONNECTION);

        int teamRolls = indexOf(PacketCommand.TEAM_INITIATIVE);
        int phaseChange = indexOf(PacketCommand.PHASE_CHANGE);
        assertTrue(teamRolls >= 0, "team rolls were not sent");
        assertTrue(teamRolls < phaseChange, "team rolls must arrive before the phase change");

        Map<?, ?> teamRollsByPlayerId = (Map<?, ?>) packetsToJoiningClient.get(teamRolls).getObject(0);
        InitiativeRoll otherTeamRoll = (InitiativeRoll) teamRollsByPlayerId.get(OTHER_CONNECTION);
        assertEquals(game.getTeamForPlayer(game.getPlayer(OTHER_CONNECTION)).getInitiative(), otherTeamRoll);
    }

    @Test
    void aClientJoiningTheLobbyIsNotSentTeamRolls() {
        game.setPhase(GamePhase.LOUNGE);

        gameManager.sendCurrentInfo(JOINING_CONNECTION);

        assertEquals(-1, indexOf(PacketCommand.TEAM_INITIATIVE));
    }
}

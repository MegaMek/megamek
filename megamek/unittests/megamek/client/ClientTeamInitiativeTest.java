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
package megamek.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.game.InitiativeRoll;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.InvalidPacketDataException;
import megamek.common.net.packets.Packet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that {@code Client.receiveTeamInitiative} stores the team initiative rolls the server sends, so a bot can see
 * which team actually won initiative (issue #9275).
 */
class ClientTeamInitiativeTest {

    private static final int FIRST_PLAYER_ID = 0;
    private static final int SECOND_PLAYER_ID = 1;

    private Client client;
    private Player firstPlayer;
    private Player secondPlayer;

    @BeforeEach
    void setUp() {
        client = new Client("Test Player", "localhost", 1234);
        client.getGame().setPhase(GamePhase.INITIATIVE_REPORT);
        firstPlayer = new Player(FIRST_PLAYER_ID, "Human");
        firstPlayer.setTeam(1);
        secondPlayer = new Player(SECOND_PLAYER_ID, "Princess");
        secondPlayer.setTeam(2);
        client.getGame().addPlayer(FIRST_PLAYER_ID, firstPlayer);
        client.getGame().addPlayer(SECOND_PLAYER_ID, secondPlayer);
    }

    private static InitiativeRoll rolledWithBonus(int bonus) {
        InitiativeRoll roll = new InitiativeRoll();
        roll.addRoll(bonus, "");
        return roll;
    }

    private Packet teamInitiativePacket(InitiativeRoll firstTeamRoll, InitiativeRoll secondTeamRoll) {
        HashMap<Integer, InitiativeRoll> teamRollsByPlayerId = new HashMap<>();
        teamRollsByPlayerId.put(FIRST_PLAYER_ID, firstTeamRoll);
        teamRollsByPlayerId.put(SECOND_PLAYER_ID, secondTeamRoll);
        return new Packet(PacketCommand.TEAM_INITIATIVE, teamRollsByPlayerId);
    }

    @Test
    void storesEachTeamsRollOnThatTeam() throws Exception {
        InitiativeRoll firstTeamRoll = rolledWithBonus(6);
        InitiativeRoll secondTeamRoll = rolledWithBonus(1);

        client.receiveTeamInitiative(teamInitiativePacket(firstTeamRoll, secondTeamRoll));

        InitiativeRoll storedFirst = client.getGame().getTeamForPlayer(firstPlayer).getInitiative();
        assertEquals(firstTeamRoll, storedFirst);
        assertEquals(secondTeamRoll, client.getGame().getTeamForPlayer(secondPlayer).getInitiative());
        // A copy, so a later change on either side cannot leak into the other.
        assertNotSame(firstTeamRoll, storedFirst);
    }

    @Test
    void keepsTheTeamRollAcrossALaterPlayerUpdate() throws Exception {
        InitiativeRoll secondTeamRoll = rolledWithBonus(1);
        client.receiveTeamInitiative(teamInitiativePacket(rolledWithBonus(6), secondTeamRoll));

        // A player update rebuilds the client's teams; the roll must carry over to the rebuilt team.
        Player updatedSecondPlayer = new Player(SECOND_PLAYER_ID, "Princess");
        updatedSecondPlayer.setTeam(2);
        client.getGame().setPlayer(SECOND_PLAYER_ID, updatedSecondPlayer);

        assertEquals(secondTeamRoll, client.getGame().getTeamForPlayer(updatedSecondPlayer).getInitiative());
    }

    @Test
    void ignoresAPlayerTheClientDoesNotKnow() throws Exception {
        HashMap<Integer, InitiativeRoll> teamRollsByPlayerId = new HashMap<>(Map.of(99, rolledWithBonus(0)));

        client.receiveTeamInitiative(new Packet(PacketCommand.TEAM_INITIATIVE, teamRollsByPlayerId));

        assertEquals(0, client.getGame().getTeamForPlayer(firstPlayer).getInitiative().size());
    }

    @Test
    void rejectsAPacketThatIsNotAMap() {
        assertThrows(InvalidPacketDataException.class,
              () -> client.receiveTeamInitiative(new Packet(PacketCommand.TEAM_INITIATIVE, "not a map")));
    }
}

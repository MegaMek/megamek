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

package megamek.server.sbf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import megamek.common.Player;
import megamek.common.actions.EntityAction;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFFormationTurn;
import megamek.common.strategicBattleSystems.SBFGame;
import megamek.common.strategicBattleSystems.SBFMovePath;
import megamek.server.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/** Tests targeted authoritative corrections for rejected SBF turn submissions. */
class SBFGameManagerCorrectionTest {

    private static final int REQUESTER_ID = 0;
    private static final int OTHER_PLAYER_ID = 1;
    private static final int REQUESTER_FORMATION_ID = 17;
    private static final int FOREIGN_FORMATION_ID = 18;

    private SBFGameManager gameManager;
    private SBFGame game;
    private SBFFormation requesterFormation;
    private SBFFormation foreignFormation;
    private Server server;

    @BeforeEach
    void setUp() {
        gameManager = new SBFGameManager();
        game = new SBFGame();
        game.addPlayer(REQUESTER_ID, new Player(REQUESTER_ID, "Requester"));
        game.addPlayer(OTHER_PLAYER_ID, new Player(OTHER_PLAYER_ID, "Other"));
        requesterFormation = formation(REQUESTER_FORMATION_ID, REQUESTER_ID);
        foreignFormation = formation(FOREIGN_FORMATION_ID, OTHER_PLAYER_ID);
        game.addUnit(requesterFormation);
        game.addUnit(foreignFormation);
        game.setTurns(List.of(new SBFFormationTurn(REQUESTER_ID)));
        game.setTurnIndex(0, Player.PLAYER_NONE);
        gameManager.setGame(game);
        server = mock(Server.class);
    }

    @Test
    void rejectedMovementInWrongPhaseRestoresOwnedFormationPhaseAndTurn() {
        game.setPhase(GamePhase.FIRING);
        BoardLocation originalPosition = requesterFormation.getPosition();
        SBFMovePath movePath = new SBFMovePath(REQUESTER_FORMATION_ID, originalPosition, game);

        List<Packet> corrections = handleAndCapture(
              new Packet(PacketCommand.ENTITY_MOVE, movePath), REQUESTER_ID);

        assertEquals(List.of(PacketCommand.ENTITY_UPDATE, PacketCommand.PHASE_CHANGE,
              PacketCommand.SENDING_TURNS, PacketCommand.TURN), commands(corrections));
        assertSame(requesterFormation, corrections.get(0).getObject(0));
        assertEquals(GamePhase.FIRING, corrections.get(1).getObject(0));
        assertEquals(game.getTurnsList(), corrections.get(2).getObject(0));
        assertEquals(game.getTurnIndex(), corrections.get(3).getObject(0));
        assertEquals(REQUESTER_ID, corrections.get(3).getObject(1));
        assertEquals(originalPosition, requesterFormation.getPosition());
        assertFalse(requesterFormation.isDone());
        assertEquals(0, game.getTurnIndex());
        verify(server, never()).send(eq(OTHER_PLAYER_ID), any(Packet.class));
    }

    @Test
    void rejectedAttackReplacesOptimisticActionsAndRestoresPhaseAndTurn() {
        game.setPhase(GamePhase.MOVEMENT);
        EntityAction authoritativeAction = mock(EntityAction.class);
        game.addAction(authoritativeAction);
        EntityAction rejectedAction = mock(EntityAction.class);
        when(rejectedAction.getEntityId()).thenReturn(REQUESTER_FORMATION_ID);

        List<Packet> corrections = handleAndCapture(new Packet(PacketCommand.ENTITY_ATTACK,
              REQUESTER_FORMATION_ID, new ArrayList<>(List.of(rejectedAction))), REQUESTER_ID);

        assertEquals(List.of(PacketCommand.ENTITY_UPDATE, PacketCommand.ACTIONS,
              PacketCommand.PHASE_CHANGE, PacketCommand.SENDING_TURNS, PacketCommand.TURN), commands(corrections));
        assertEquals(List.of(authoritativeAction), corrections.get(1).getObject(0));
        assertEquals(GamePhase.MOVEMENT, corrections.get(2).getObject(0));
        assertEquals(REQUESTER_ID, corrections.get(4).getObject(1));
        assertFalse(requesterFormation.isDone());
        assertEquals(0, game.getTurnIndex());
        verify(server, never()).send(eq(OTHER_PLAYER_ID), any(Packet.class));
    }

    @Test
    void rejectedForeignFormationDoesNotDiscloseItsState() {
        game.setPhase(GamePhase.MOVEMENT);
        SBFMovePath movePath = new SBFMovePath(FOREIGN_FORMATION_ID, foreignFormation.getPosition(), game);

        List<Packet> corrections = handleAndCapture(
              new Packet(PacketCommand.ENTITY_MOVE, movePath), REQUESTER_ID);

        assertEquals(List.of(PacketCommand.SENDING_TURNS, PacketCommand.TURN), commands(corrections));
        assertEquals(REQUESTER_ID, corrections.get(1).getObject(1));
        assertEquals(0, game.getTurnIndex());
        verify(server, never()).send(eq(OTHER_PLAYER_ID), any(Packet.class));
    }

    @Test
    void wrongPhaseForeignMovementRestoresPhaseWithoutDisclosingItsState() {
        game.setPhase(GamePhase.FIRING);
        SBFMovePath movePath = new SBFMovePath(FOREIGN_FORMATION_ID, foreignFormation.getPosition(), game);

        List<Packet> corrections = handleAndCapture(
              new Packet(PacketCommand.ENTITY_MOVE, movePath), REQUESTER_ID);

        assertEquals(List.of(PacketCommand.PHASE_CHANGE, PacketCommand.SENDING_TURNS,
              PacketCommand.TURN), commands(corrections));
    }

    @Test
    void malformedMovementRestoresPhaseAndTurnWithoutDisclosingAUnit() {
        game.setPhase(GamePhase.MOVEMENT);

        List<Packet> corrections = handleAndCapture(
              new Packet(PacketCommand.ENTITY_MOVE, "not a move path"), REQUESTER_ID);

        assertEquals(List.of(PacketCommand.PHASE_CHANGE, PacketCommand.SENDING_TURNS,
              PacketCommand.TURN), commands(corrections));
    }

    @Test
    void malformedAttackRestoresActionsPhaseAndTurnWithoutDisclosingAUnit() {
        game.setPhase(GamePhase.FIRING);
        EntityAction authoritativeAction = mock(EntityAction.class);
        game.addAction(authoritativeAction);

        List<Packet> corrections = handleAndCapture(
              new Packet(PacketCommand.ENTITY_ATTACK, "not an id", "not actions"), REQUESTER_ID);

        assertEquals(List.of(PacketCommand.ACTIONS, PacketCommand.PHASE_CHANGE,
              PacketCommand.SENDING_TURNS, PacketCommand.TURN), commands(corrections));
        assertEquals(List.of(authoritativeAction), corrections.get(0).getObject(0));
    }

    private SBFFormation formation(int id, int ownerId) {
        SBFFormation formation = new SBFFormation();
        formation.setId(id);
        formation.setOwnerId(ownerId);
        formation.setDeployed(true);
        formation.setPosition(BoardLocation.of(new Coords(id, id), 0));
        return formation;
    }

    private List<Packet> handleAndCapture(Packet request, int requesterId) {
        try (MockedStatic<Server> serverStatic = mockStatic(Server.class)) {
            serverStatic.when(Server::getServerInstance).thenReturn(server);
            gameManager.handlePacket(requesterId, request);
        }

        ArgumentCaptor<Packet> envelopeCaptor = ArgumentCaptor.forClass(Packet.class);
        verify(server).send(eq(requesterId), envelopeCaptor.capture());
        Packet envelope = envelopeCaptor.getValue();
        assertEquals(PacketCommand.MULTI_PACKET, envelope.command());
        @SuppressWarnings("unchecked")
        List<Packet> corrections = (List<Packet>) envelope.getObject(0);
        return corrections;
    }

    private List<PacketCommand> commands(List<Packet> packets) {
        return packets.stream().map(Packet::command).toList();
    }
}

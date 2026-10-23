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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.ICarryable;
import megamek.common.equipment.ObjectiveMarker;
import megamek.common.game.Game;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * A Scan point's note ("The scan reveals...") is kept from the other side until they scan it: their copy of the
 * board comes without it, and sending that copy back during Victory Setup neither wipes the note nor lets them
 * rewrite it. Driven through the packets the server really sends and receives.
 */
class ScanNotePrivacyTest {

    private static final Coords FACTORY = new Coords(3, 3);
    private static final String NOTE = "The warehouse is full of PPC capacitors.";

    private static final int ALICE = 0;
    private static final int BOB = 1;
    private static final int CAROL = 2;
    private static final int REFEREE = 3;

    private TWGameManager gameManager;
    private Game game;

    @BeforeEach
    void beforeEach() {
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).send(anyInt(), any(Packet.class));
        Mockito.doNothing().when(gameManager).endCurrentTurn(any());
        game = gameManager.getGame();
        addPlayer(ALICE, "Alice", 1, false);
        addPlayer(BOB, "Bob", 2, false);
        addPlayer(CAROL, "Carol", 1, false);
        addPlayer(REFEREE, "Referee", 3, true);
        game.setPhase(GamePhase.VICTORY_SETUP);
        game.placeGroundObject(FACTORY, scanPoint(ALICE, NOTE));
    }

    // --- what each player is sent ---

    @Test
    void theOtherSideIsSentThePointWithoutItsNote() {
        gameManager.sendGroundObjectUpdate();

        ObjectiveMarker bobsCopy = markerSentTo(BOB);
        assertEquals("", bobsCopy.getScanRevealsNote());
        assertEquals(ALICE, bobsCopy.getOwnerId(), "the point itself still reaches them");
    }

    @Test
    void thePlacerTheirTeamAndAGameMasterAreSentTheNote() {
        gameManager.sendGroundObjectUpdate();

        assertEquals(NOTE, markerSentTo(ALICE).getScanRevealsNote());
        assertEquals(NOTE, markerSentTo(CAROL).getScanRevealsNote(), "a teammate may read it");
        assertEquals(NOTE, markerSentTo(REFEREE).getScanRevealsNote());
    }

    @Test
    void withholdingTheNoteLeavesTheServersPointAlone() {
        gameManager.sendGroundObjectUpdate();

        assertEquals(NOTE, storedMarker().getScanRevealsNote());
    }

    @Test
    void aReconnectingPlayerIsSentTheirOwnCopy() {
        new ScanNotePrivacyHandler(gameManager).sendGroundObjectsTo(BOB);

        assertEquals("", markerSentTo(BOB).getScanRevealsNote());
    }

    // --- what comes back at the end of a Victory Setup turn ---

    @Test
    void theOtherSideEndingTheirTurnDoesNotWipeTheNote() {
        gameManager.handlePacket(BOB, setupTurn(scanPoint(ALICE, "")));

        assertEquals(NOTE, storedMarker().getScanRevealsNote());
    }

    @Test
    void theOtherSideCannotRewriteTheNote() {
        gameManager.handlePacket(BOB, setupTurn(scanPoint(ALICE, "Nothing here.")));

        assertEquals(NOTE, storedMarker().getScanRevealsNote());
    }

    @Test
    void relabellingThePointAsTheirOwnDoesNotLetTheOtherSideRewriteTheNote() {
        // a modified client could claim the point is theirs; the placer is taken from the server's point
        gameManager.handlePacket(BOB, setupTurn(scanPoint(BOB, "Nothing here.")));

        assertEquals(NOTE, storedMarker().getScanRevealsNote());
    }

    @Test
    void thePlacerCanChangeTheirNote() {
        gameManager.handlePacket(ALICE, setupTurn(scanPoint(ALICE, "Empty crates.")));

        assertEquals("Empty crates.", storedMarker().getScanRevealsNote());
    }

    @Test
    void aGameMasterCanChangeAnyNote() {
        gameManager.handlePacket(REFEREE, setupTurn(scanPoint(ALICE, "Empty crates.")));

        assertEquals("Empty crates.", storedMarker().getScanRevealsNote());
    }

    @Test
    void theOtherSidesOwnNewPointKeepsTheirNote() {
        Map<Coords, List<ICarryable>> board = boardWith(scanPoint(ALICE, ""));
        Coords bunker = new Coords(5, 5);
        board.put(bunker, new ArrayList<>(List.of(scanPoint(BOB, "Spare ammunition."))));

        gameManager.handlePacket(BOB, new Packet(PacketCommand.UPDATE_GROUND_OBJECTS, board));

        assertEquals(NOTE, storedMarker().getScanRevealsNote());
        ObjectiveMarker bobsPoint = (ObjectiveMarker) game.getGroundObjects(bunker).getFirst();
        assertEquals("Spare ammunition.", bobsPoint.getScanRevealsNote());
    }

    // --- the shared rule ---

    @Test
    void onlyThePlacersSideAndAGameMasterMayReadANote() {
        Player alice = game.getPlayer(ALICE);

        assertTrue(ObjectiveMarker.isScanNoteVisibleTo(alice, alice));
        assertTrue(ObjectiveMarker.isScanNoteVisibleTo(alice, game.getPlayer(CAROL)));
        assertTrue(ObjectiveMarker.isScanNoteVisibleTo(alice, game.getPlayer(REFEREE)));
        assertFalse(ObjectiveMarker.isScanNoteVisibleTo(alice, game.getPlayer(BOB)));
    }

    @Test
    void aNoteWhosePlacerHasLeftIsForAGameMasterOnly() {
        assertTrue(ObjectiveMarker.isScanNoteVisibleTo(null, game.getPlayer(REFEREE)));
        assertFalse(ObjectiveMarker.isScanNoteVisibleTo(null, game.getPlayer(BOB)));
    }

    @Test
    void theCopyWithoutTheNoteLeavesTheOriginalAlone() {
        ObjectiveMarker original = scanPoint(ALICE, NOTE);

        ObjectiveMarker copy = original.copyWithoutScanNote();

        assertEquals("", copy.getScanRevealsNote());
        assertEquals(NOTE, original.getScanRevealsNote());
        assertEquals(original.getVictoryPointValue(), copy.getVictoryPointValue());
    }

    // --- helpers ---

    private void addPlayer(int id, String name, int team, boolean isGameMaster) {
        Player player = new Player(id, name);
        player.setTeam(team);
        player.setGameMaster(isGameMaster);
        game.addPlayer(id, player);
    }

    private static ObjectiveMarker scanPoint(int placerId, String note) {
        ObjectiveMarker marker = new ObjectiveMarker();
        marker.setName("Factory");
        marker.setOwnerId(placerId);
        marker.setVictoryPointValue(5);
        marker.setScanRevealsNote(note);
        return marker;
    }

    private static Map<Coords, List<ICarryable>> boardWith(ObjectiveMarker factoryPoint) {
        Map<Coords, List<ICarryable>> board = new HashMap<>();
        board.put(FACTORY, new ArrayList<>(List.of(factoryPoint)));
        return board;
    }

    /** @return the packet a player's client sends when they press Done in Victory Setup */
    private static Packet setupTurn(ObjectiveMarker factoryPoint) {
        return new Packet(PacketCommand.UPDATE_GROUND_OBJECTS, boardWith(factoryPoint));
    }

    private ObjectiveMarker storedMarker() {
        return (ObjectiveMarker) game.getGroundObjects(FACTORY).getFirst();
    }

    /** @return the point at the factory in the last ground object packet sent to one connection */
    @SuppressWarnings("unchecked")
    private ObjectiveMarker markerSentTo(int connectionId) {
        ArgumentCaptor<Packet> sent = ArgumentCaptor.forClass(Packet.class);
        verify(gameManager, atLeastOnce()).send(eq(connectionId), sent.capture());
        Packet groundObjectPacket = null;
        for (Packet packet : sent.getAllValues()) {
            if (packet.command() == PacketCommand.UPDATE_GROUND_OBJECTS) {
                groundObjectPacket = packet;
            }
        }
        assertNotNull(groundObjectPacket, "no ground objects were sent to connection " + connectionId);
        Map<Coords, List<ICarryable>> board = (Map<Coords, List<ICarryable>>) groundObjectPacket.getObject(0);
        return (ObjectiveMarker) board.get(FACTORY).getFirst();
    }
}

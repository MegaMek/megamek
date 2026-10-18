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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.ICarryable;
import megamek.common.equipment.ObjectiveMarker;
import megamek.common.equipment.ObjectiveScoringScheme;
import megamek.common.game.Game;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * The server half of the objective variants editor. Potential and False cannot be combined, a Scan point cannot be
 * Potential and only a Raid point can be Mobile, and the editor never offers a forbidden tick. These tests send
 * markers the way a client does, through {@code handlePacket}, and check that the server drops a forbidden flag on
 * both ways an edited point arrives: a player's Victory Setup turn and a game master's in-game edit. Allowed markers
 * are shown to arrive untouched, so the correction cannot pass by clearing every flag.
 */
class ObjectiveVariantPacketTest {

    private static final String BOARD_DATA = """
          size 8 8
          end""";
    private static final int GAME_MASTER_CONNECTION = 0;
    private static final int PLAYER_CONNECTION = 1;

    private TWGameManager gameManager;
    private Game game;
    private final Coords pointHex = new Coords(4, 4);

    @BeforeEach
    void setUp() {
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).send(Mockito.anyInt(), any(Packet.class));
        Mockito.doNothing().when(gameManager).sendServerChat(Mockito.anyString());
        Mockito.doNothing().when(gameManager).endCurrentTurn(any());
        game = gameManager.getGame();
        Player gameMaster = new Player(GAME_MASTER_CONNECTION, "Referee");
        gameMaster.setGameMaster(true);
        game.addPlayer(GAME_MASTER_CONNECTION, gameMaster);
        game.addPlayer(PLAYER_CONNECTION, new Player(PLAYER_CONNECTION, "Player"));
        Board board = BoardLoader.initializeBoard(BOARD_DATA);
        game.setBoard(board);
        game.getOptions().getOption(OptionsConstants.VICTORY_USE_OBJECTIVES).setValue(true);
    }

    /** A Raid point, the one scheme that allows every variant, so only the Potential and False rule is in play. */
    private static ObjectiveMarker markerWith(boolean isPotential, boolean isFalse) {
        ObjectiveMarker marker = new ObjectiveMarker();
        marker.setName("Supply Cache");
        marker.setOwnerId(PLAYER_CONNECTION);
        marker.setScoringScheme(ObjectiveScoringScheme.raid());
        marker.setPotential(isPotential);
        marker.setFalseObjective(isFalse);
        marker.setFragile(true);
        marker.setMobile(true);
        return marker;
    }

    private void sendVictorySetupTurn(ObjectiveMarker marker) {
        game.setPhase(GamePhase.VICTORY_SETUP);
        List<ICarryable> hexObjects = new ArrayList<>();
        hexObjects.add(marker);
        Map<Coords, List<ICarryable>> groundObjects = new HashMap<>();
        groundObjects.put(pointHex, hexObjects);
        gameManager.handlePacket(PLAYER_CONNECTION, new Packet(PacketCommand.UPDATE_GROUND_OBJECTS, groundObjects));
    }

    private void sendGameMasterEdit(ObjectiveMarker marker) {
        game.setPhase(GamePhase.MOVEMENT);
        gameManager.handlePacket(GAME_MASTER_CONNECTION, new Packet(PacketCommand.OBJECTIVE_EDIT, pointHex, marker));
    }

    private ObjectiveMarker storedMarker() {
        for (ICarryable groundObject : game.getGroundObjects(pointHex)) {
            if (groundObject instanceof ObjectiveMarker marker) {
                return marker;
            }
        }
        throw new AssertionError("no objective was stored at " + pointHex);
    }

    @Test
    void testAVictorySetupTurnWithPotentialAndFalseKeepsOnlyPotential() {
        sendVictorySetupTurn(markerWith(true, true));

        ObjectiveMarker stored = storedMarker();
        assertTrue(stored.isPotential(), "Potential is the flag kept");
        assertFalse(stored.isFalseObjective(), "False is dropped");
        assertTrue(stored.isFragile(), "the other variants are left alone");
        assertTrue(stored.isMobile());
    }

    @Test
    void testAVictorySetupTurnWithFalseAloneArrivesUntouched() {
        sendVictorySetupTurn(markerWith(false, true));

        ObjectiveMarker stored = storedMarker();
        assertTrue(stored.isFalseObjective());
        assertFalse(stored.isPotential());
        assertTrue(stored.isFragile());
        assertTrue(stored.isMobile());
    }

    @Test
    void testAGameMasterEditWithPotentialAndFalseKeepsOnlyPotential() {
        sendGameMasterEdit(markerWith(true, true));

        ObjectiveMarker stored = storedMarker();
        assertTrue(stored.isPotential());
        assertFalse(stored.isFalseObjective());
    }

    @Test
    void testAPotentialScanPointLosesPotential() {
        ObjectiveMarker marker = markerWith(true, false);
        marker.setScoringScheme(ObjectiveScoringScheme.scan(ObjectiveScoringScheme.ScanPayout.ON_SCAN));
        marker.setMobile(false);

        sendVictorySetupTurn(marker);

        ObjectiveMarker stored = storedMarker();
        assertFalse(stored.isPotential(), "a Scan point cannot be Potential");
        assertTrue(stored.isFragile(), "the other variants are left alone");
    }

    @Test
    void testAScanPointSentAsPotentialAndFalseKeepsFalse() {
        ObjectiveMarker marker = markerWith(true, true);
        marker.setScoringScheme(ObjectiveScoringScheme.scan(ObjectiveScoringScheme.ScanPayout.ON_SCAN));
        marker.setMobile(false);

        sendVictorySetupTurn(marker);

        ObjectiveMarker stored = storedMarker();
        assertFalse(stored.isPotential(), "the scheme rule removes Potential first");
        assertTrue(stored.isFalseObjective(), "so False, the flag a Scan point can have, survives");
    }

    @Test
    void testAMobilePointThatIsNotARaidPointLosesMobile() {
        ObjectiveMarker marker = markerWith(false, false);
        marker.setScoringScheme(ObjectiveScoringScheme.standard());

        sendGameMasterEdit(marker);

        ObjectiveMarker stored = storedMarker();
        assertFalse(stored.isMobile(), "only a Raid point can be Mobile");
        assertTrue(stored.isFragile());
    }

    @Test
    void testAGameMasterEditWithPotentialAloneArrivesUntouched() {
        sendGameMasterEdit(markerWith(true, false));

        ObjectiveMarker stored = storedMarker();
        assertTrue(stored.isPotential());
        assertFalse(stored.isFalseObjective());
        assertTrue(stored.isFragile());
        assertTrue(stored.isMobile());
    }
}

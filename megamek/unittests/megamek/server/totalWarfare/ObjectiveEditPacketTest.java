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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import java.util.List;

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
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * The packet path of a game master's in-game objective edit: sent the way a client sends it, through
 * {@code handlePacket}, the edit replaces the marker at the hex or removes it, every client is told, and a player
 * who is not a game master changes nothing.
 */
class ObjectiveEditPacketTest {

    private static final String BOARD_DATA = """
          size 8 8
          end""";
    private static final int GAME_MASTER_CONNECTION = 0;
    private static final int PLAYER_CONNECTION = 1;

    private TWGameManager gameManager;
    private Game game;
    private Coords pointHex;
    private ObjectiveMarker original;
    private BipedMek convoyTruck;

    @BeforeEach
    void setUp() {
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).send(Mockito.anyInt(), any(Packet.class));
        Mockito.doNothing().when(gameManager).sendServerChat(Mockito.anyString());
        game = gameManager.getGame();
        Player gameMaster = new Player(GAME_MASTER_CONNECTION, "Referee");
        gameMaster.setGameMaster(true);
        Player player = new Player(PLAYER_CONNECTION, "Player");
        game.addPlayer(GAME_MASTER_CONNECTION, gameMaster);
        game.addPlayer(PLAYER_CONNECTION, player);
        Board board = BoardLoader.initializeBoard(BOARD_DATA);
        game.setBoard(board);
        game.getOptions().getOption(OptionsConstants.VICTORY_USE_OBJECTIVES).setValue(true);
        game.setPhase(GamePhase.MOVEMENT);

        pointHex = new Coords(4, 4);
        original = new ObjectiveMarker();
        original.setName("Depot");
        original.setOwnerId(PLAYER_CONNECTION);
        original.setVictoryPointValue(1);
        original.setScoringScheme(ObjectiveScoringScheme.scan(ObjectiveScoringScheme.ScanPayout.ON_EXIT));
        game.placeGroundObject(pointHex, original);

        convoyTruck = new BipedMek();
        convoyTruck.setGame(game);
        convoyTruck.setId(game.getNextEntityId());
        convoyTruck.setChassis("Convoy");
        convoyTruck.setModel("T-1");
        convoyTruck.setCrew(new Crew(CrewType.SINGLE));
        convoyTruck.setOwner(player);
        convoyTruck.setPosition(new Coords(2, 2));
        convoyTruck.setDeployed(true);
        game.addEntity(convoyTruck);
    }

    private ObjectiveMarker editedCopy() {
        ObjectiveMarker edited = new ObjectiveMarker();
        edited.setName("Listening Post");
        edited.setOwnerId(PLAYER_CONNECTION);
        edited.setVictoryPointValue(3);
        edited.setScoringScheme(ObjectiveScoringScheme.scan(ObjectiveScoringScheme.ScanPayout.ON_SCAN));
        edited.setScanRevealsNote("The post is abandoned.");
        return edited;
    }

    private List<ObjectiveMarker> markersAtThePoint() {
        return game.getGroundObjects(pointHex).stream()
              .filter(ObjectiveMarker.class::isInstance)
              .map(ObjectiveMarker.class::cast)
              .toList();
    }

    @Test
    void testAGameMastersEditReplacesTheMarkerAndTellsEveryClient() {
        ObjectiveMarker edited = editedCopy();

        gameManager.handlePacket(GAME_MASTER_CONNECTION, new Packet(PacketCommand.OBJECTIVE_EDIT, pointHex, edited));

        List<ObjectiveMarker> markers = markersAtThePoint();
        assertEquals(1, markers.size(), "the old marker is gone, the edited one is there");
        assertSame(edited, markers.getFirst());
        assertEquals("The post is abandoned.", markers.getFirst().getScanRevealsNote());
        Mockito.verify(gameManager).sendGroundObjectUpdate();
    }

    @Test
    void testAGameMasterCanRemoveTheObjective() {
        gameManager.handlePacket(GAME_MASTER_CONNECTION, new Packet(PacketCommand.OBJECTIVE_EDIT, pointHex, null));

        assertTrue(markersAtThePoint().isEmpty(), "nothing is left at the hex");
        Mockito.verify(gameManager).sendGroundObjectUpdate();
    }

    @Test
    void testAGameMasterCanPlaceAnObjectiveOnAnEmptyHex() {
        Coords emptyHex = new Coords(1, 1);
        ObjectiveMarker placed = editedCopy();

        gameManager.handlePacket(GAME_MASTER_CONNECTION, new Packet(PacketCommand.OBJECTIVE_EDIT, emptyHex, placed));

        List<ICarryable> atTheHex = game.getGroundObjects(emptyHex);
        assertEquals(1, atTheHex.size());
        assertSame(placed, atTheHex.getFirst());
        assertEquals(1, markersAtThePoint().size(), "the other point is untouched");
    }

    @Test
    void testAGameMasterCanMarkAUnitAsWantedForScanning() {
        gameManager.handlePacket(GAME_MASTER_CONNECTION,
              new Packet(PacketCommand.SCAN_DESIGNATION, convoyTruck.getId(), true));

        assertTrue(convoyTruck.isDesignatedScanTarget(), "the mission now wants this vehicle read");
        Mockito.verify(gameManager).entityUpdate(convoyTruck.getId());
    }

    @Test
    void testAGameMasterCanTakeTheMarkingBackOff() {
        convoyTruck.setDesignatedScanTarget(true);

        gameManager.handlePacket(GAME_MASTER_CONNECTION,
              new Packet(PacketCommand.SCAN_DESIGNATION, convoyTruck.getId(), false));

        assertFalse(convoyTruck.isDesignatedScanTarget(), "the request is dropped");
    }

    @Test
    void testAPlayerWhoIsNotAGameMasterCannotMarkAUnit() {
        gameManager.handlePacket(PLAYER_CONNECTION,
              new Packet(PacketCommand.SCAN_DESIGNATION, convoyTruck.getId(), true));

        assertFalse(convoyTruck.isDesignatedScanTarget(), "only a game master sets the mission's targets");
    }

    @Test
    void testAMarkingForAUnitThatIsNotThereChangesNothing() {
        gameManager.handlePacket(GAME_MASTER_CONNECTION,
              new Packet(PacketCommand.SCAN_DESIGNATION, 9999, true));

        assertFalse(convoyTruck.isDesignatedScanTarget());
    }

    @Test
    void testAPlayerWhoIsNotAGameMasterChangesNothing() {
        gameManager.handlePacket(PLAYER_CONNECTION, new Packet(PacketCommand.OBJECTIVE_EDIT, pointHex, editedCopy()));

        List<ObjectiveMarker> markers = markersAtThePoint();
        assertEquals(1, markers.size());
        assertSame(original, markers.getFirst(), "the original marker stands");
        Mockito.verify(gameManager, Mockito.never()).sendGroundObjectUpdate();
    }
}

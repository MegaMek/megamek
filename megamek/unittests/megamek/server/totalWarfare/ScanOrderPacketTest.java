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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import megamek.common.Player;
import megamek.common.actions.ScanAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
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
 * The scan order's packet path: the order a client sends in the pre-End declarations phase reaches the unit it names,
 * only from the connection that owns that unit, and only in that phase. Sent the way a client sends it, through
 * {@code handlePacket}, so the wiring from the packet command to the handler is what is tested.
 */
class ScanOrderPacketTest {

    private static final String BOARD_DATA = """
          size 8 8
          end""";
    private static final int OWNER_CONNECTION = 0;
    private static final int OTHER_CONNECTION = 1;

    private TWGameManager gameManager;
    private Game game;
    private BipedMek scout;
    private Coords pointHex;

    @BeforeEach
    void setUp() {
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).send(Mockito.anyInt(), any(Packet.class));
        // a refused order is told to its sender in chat, which needs a live server outside a test
        Mockito.doNothing().when(gameManager).sendServerChat(Mockito.anyInt(), Mockito.anyString());
        game = gameManager.getGame();
        Player owner = new Player(OWNER_CONNECTION, "Owner");
        owner.setTeam(1);
        Player other = new Player(OTHER_CONNECTION, "Other");
        other.setTeam(2);
        game.addPlayer(OWNER_CONNECTION, owner);
        game.addPlayer(OTHER_CONNECTION, other);
        Board board = BoardLoader.initializeBoard(BOARD_DATA);
        game.setBoard(board);
        game.getOptions().getOption(OptionsConstants.VICTORY_USE_OBJECTIVES).setValue(true);
        game.setPhase(GamePhase.PREEND_DECLARATIONS);

        // a scan point of the owner's, so scanning is in play at all
        pointHex = new Coords(4, 4);
        ObjectiveMarker scanPoint = new ObjectiveMarker();
        scanPoint.setName("Depot");
        scanPoint.setOwnerId(OWNER_CONNECTION);
        scanPoint.setScoringScheme(ObjectiveScoringScheme.scan(true));
        game.placeGroundObject(pointHex, scanPoint);

        scout = new BipedMek();
        scout.setGame(game);
        scout.setId(game.getNextEntityId());
        scout.setChassis("Scout");
        scout.setModel("S-1");
        scout.setCrew(new Crew(CrewType.SINGLE));
        scout.setOwner(owner);
        scout.setPosition(new Coords(4, 2));
        scout.setDeployed(true);
        game.addEntity(scout);
    }

    @Test
    void testTheOwnersOrderReachesTheUnit() {
        gameManager.handlePacket(OWNER_CONNECTION,
              new Packet(PacketCommand.ENTITY_SCAN_ORDER, new ScanAction(scout.getId(), pointHex, 0)));

        ScanAction pending = scout.getPendingScan();
        assertNotNull(pending, "the order is stored on the unit for the End Phase");
        assertEquals(pointHex, pending.getTargetPosition());
    }

    @Test
    void testAnotherPlayersOrderForTheUnitChangesNothing() {
        gameManager.handlePacket(OTHER_CONNECTION,
              new Packet(PacketCommand.ENTITY_SCAN_ORDER, new ScanAction(scout.getId(), pointHex, 0)));

        assertNull(scout.getPendingScan(), "only the unit's owner may give it an order");
    }

    @Test
    void testAnOrderOutsideThePreEndPhaseChangesNothing() {
        game.setPhase(GamePhase.MOVEMENT);

        gameManager.handlePacket(OWNER_CONNECTION,
              new Packet(PacketCommand.ENTITY_SCAN_ORDER, new ScanAction(scout.getId(), pointHex, 0)));

        assertNull(scout.getPendingScan());
    }

    @Test
    void testAnOrderOutOfRangeIsRefused() {
        // 2 hexes is the Core default and the scout has no probe: 6 hexes away is out of reach
        gameManager.handlePacket(OWNER_CONNECTION,
              new Packet(PacketCommand.ENTITY_SCAN_ORDER, new ScanAction(scout.getId(), new Coords(4, 8), 0)));

        assertNull(scout.getPendingScan());
    }

    @Test
    void testAUnitThatCanScanGetsAPreEndTurnAndSharesItWithItsPlayer() {
        assertTrue(scout.isEligibleForPreEndDeclarations(), "scanning is in play, so the phase runs for it");
        assertFalse(scout.hasEntityScopedPreEndDeclaration(), "one turn per player, every scanner ordered in it");

        game.getOptions().getOption(OptionsConstants.VICTORY_USE_OBJECTIVES).setValue(false);
        assertFalse(scout.isEligibleForPreEndDeclarations(), "objectives off: nothing to scan for");
    }
}

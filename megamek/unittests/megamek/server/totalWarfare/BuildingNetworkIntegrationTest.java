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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import javax.swing.SwingUtilities;

import megamek.client.Client;
import megamek.common.Player;
import megamek.common.bays.MekBay;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BuildingType;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.units.BuildingBayDoors;
import megamek.common.units.BuildingDesign;
import megamek.common.units.IBuilding;
import megamek.common.units.MobileStructure;
import megamek.common.util.SerializationHelper;
import megamek.server.Server;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Exercises actual TCP clients, incremental building/entity packets, and a save-game server restart. */
class BuildingNetworkIntegrationTest {
    private static final int BUILDING_ID = 17;
    private static final Coords ORIGIN = new Coords(5, 5);
    private static final BuildingDesign.BayDoor DOOR =
          new BuildingDesign.BayDoor(7, new BuildingDesign.Position(CubeCoords.ZERO, 0), 0);
    private final List<Peer> peers = new ArrayList<>();
    private Server server;

    private static final class Peer extends Client {
        private Predicate<Game> expected = game -> false;
        private CountDownLatch received = new CountDownLatch(1);

        Peer(String name, int port) { super(name, "localhost", port); }

        @Override protected boolean keepGameLog() { return false; }

        @Override protected void handlePacket(Packet packet) {
            super.handlePacket(packet);
            if (expected.test(getGame())) { received.countDown(); }
        }

        void awaitState(Predicate<Game> condition, String description) throws Exception {
            var latch = new CountDownLatch(1);
            SwingUtilities.invokeAndWait(() -> {
                expected = condition;
                received = latch;
                if (condition.test(getGame())) { latch.countDown(); }
            });
            assertTrue(latch.await(15, TimeUnit.SECONDS), description);
        }
    }

    private Peer connect(String name) {
        var peer = new Peer(name, server.getPort());
        peers.add(peer);
        assertTrue(peer.connect(), "local test client must connect");
        return peer;
    }

    @AfterEach void stop() throws Exception {
        for (var peer : peers) { peer.die(); }
        peers.clear();
        if (server != null) { server.die(); server = null; }
        SwingUtilities.invokeAndWait(() -> { });
    }

    @Test void twoClientsReceiveFloorDamageMovedDoorsAndTheSavedStateAfterReconnecting() throws Exception {
        EquipmentType.initializeTypes();
        var manager = new TWGameManager();
        var game = manager.getGame();
        game.setBoard(BoardLoader.initializeBoard("size 16 17\nend\n"));
        game.setPhase(GamePhase.LOUNGE);
        game.setRoundCount(3);
        game.getOptions().getOption(OptionsConstants.ADVANCED_BUILDING_EXPANDED_CF).setValue(true);
        for (int id = 0; id < 2; id++) {
            var player = new Player(id, id == 0 ? "Building owner" : "Building observer");
            player.setGhost(true);
            player.setObserver(id == 1);
            game.addPlayer(id, player);
        }
        server = new Server(null, 0, manager);
        var mobile = new MobileStructure(BuildingType.HEAVY, IBuilding.FORTRESS);
        mobile.configureConstruction(BuildingType.HEAVY, IBuilding.FORTRESS, 3, 90, 0,
              List.of(CubeCoords.ZERO, new CubeCoords(1, 0, -1)));
        mobile.setId(BUILDING_ID);
        mobile.setOwner(game.getPlayer(0));
        mobile.setDeployed(true);
        mobile.addTransporter(new MekBay(2, 1, 7));
        mobile.getDesign().getBayDoors().add(DOOR);
        game.addEntity(mobile);
        mobile.setPosition(ORIGIN);
        mobile.updateBuildingEntityHexes(0, manager);
        mobile.getInternalBuilding().enableExpandedCF();
        game.setPhase(GamePhase.MOVEMENT);
        var owner = connect("Building owner");
        var observer = connect("Building observer");
        for (var peer : List.of(owner, observer)) {
            peer.awaitState(state -> state.getEntity(BUILDING_ID) instanceof MobileStructure b
                  && b.getPosition().equals(ORIGIN), "initial mobile snapshot");
            SwingUtilities.invokeAndWait(() -> assertSame(peer.getGame().getEntity(BUILDING_ID),
                  peer.getGame().getBoard(0).getBuildingAt(ORIGIN), "initial board and entity snapshot must agree"));
        }

        mobile.setCurrentCF(64, ORIGIN, 1);
        mobile.getFloorState(ORIGIN).setArmor(2, 7);
        manager.sendChangedBuildings(new Vector<>(List.of(mobile)));
        for (var peer : List.of(owner, observer)) {
            peer.awaitState(state -> state.getEntity(BUILDING_ID) instanceof MobileStructure b
                  && b.getCurrentCF(ORIGIN, 1) == 64 && b.getFloorState(ORIGIN).getArmor(2) == 7,
                  "incremental BLDG_UPDATE must reach both clients");
        }

        assertTrue(BuildingBayDoors.damage(mobile, mobile.getTransportBays().getFirst(), DOOR));
        Coords moved = ORIGIN.translated(2);
        new MobileStructureMovementHandler(manager).relocate(mobile, moved, 1, 0);
        manager.entityUpdate(BUILDING_ID);
        for (var peer : List.of(owner, observer)) {
            peer.awaitState(state -> savedStateMatches(state, moved), "moved entity and physical door damage");
            SwingUtilities.invokeAndWait(() -> {
                var received = (MobileStructure) peer.getGame().getEntity(BUILDING_ID);
                assertSame(received, peer.getGame().getBoard(received).getBuildingAt(moved));
                assertNotSame(mobile, received);
                assertEquals(90, received.getCurrentCF(moved, 0));
            });
        }

        String saved = SerializationHelper.getSaveGameXStream().toXML(game);
        stop();
        var restoredManager = new TWGameManager();
        restoredManager.setGame((Game) SerializationHelper.getLoadSaveGameXStream().fromXML(saved));
        restoredManager.getGame().getPlayersList().forEach(player -> player.setGhost(true));
        server = new Server(null, 0, restoredManager);
        for (String name : List.of("Building owner", "Building observer")) {
            var peer = connect(name);
            peer.awaitState(state -> savedStateMatches(state, moved), "reconnect to the saved game");
            SwingUtilities.invokeAndWait(() -> {
                var restored = (MobileStructure) peer.getGame().getEntity(BUILDING_ID);
                assertSame(restored, peer.getGame().getBoard(restored).getBuildingAt(moved));
                assertEquals(1, restored.getFacing());
                assertEquals(0, BuildingBayDoors.usableDoors(restored, restored.getTransportBays().getFirst()));
            });
        }
    }

    private static boolean savedStateMatches(Game state, Coords position) {
        return state.getEntity(BUILDING_ID) instanceof MobileStructure building && position.equals(building.getPosition())
              && building.getCurrentCF(position, 1) == 64 && building.getFloorState(position).getArmor(2) == 7
              && building.getBuildingRuntimeState().getDamagedBayDoors().getOrDefault(DOOR, 0) == 1;
    }
}

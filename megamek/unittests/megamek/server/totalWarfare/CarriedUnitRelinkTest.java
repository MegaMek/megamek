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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.ExternalCargo;
import megamek.common.equipment.ICarryable;
import megamek.common.equipment.Transporter;
import megamek.common.game.Game;
import megamek.common.loaders.MekFileParser;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.server.Server;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Units carried in a Mek's arms are stored as objects, not ids. When a lobby edit replaces one of those units with the
 * client's copy, the carrier must be pointed at the new copy, or dropping the cargo places the old copy and the game's
 * unit stays carried, off the board (#9335).
 */
class CarriedUnitRelinkTest {

    private static final int CONNECTION_ID = 0;
    private static final int CARRIER_ID = 1;
    private static final int ELEMENTALS_ID = 2;
    private static final Coords DROP_HEX = new Coords(2, 2);

    private TWGameManager gameManager;
    private Game game;
    private MockedStatic<Server> mockedServer;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() throws Exception {
        mockedServer = mockStatic(Server.class);
        mockedServer.when(Server::getServerInstance).thenReturn(mock(Server.class));
        gameManager = Mockito.spy(new TWGameManager());
        game = gameManager.getGame();
        game.setBoard(BoardLoader.initializeBoard("size 5 5\nend"));
        game.addPlayer(CONNECTION_ID, new Player(CONNECTION_ID, "Tester"));
        game.getOptions().getOption(OptionsConstants.ADVANCED_COMBAT_PICKING_UP_AND_THROWING_UNITS).setValue(true);
        game.setPhase(GamePhase.LOUNGE);

        Entity carrier = addUnit("Atlas AS7-D.mtf", CARRIER_ID);
        Entity elementals = addUnit("Elemental BA [Laser] (Sqd5).blk", ELEMENTALS_ID);
        gameManager.loadUnit(carrier, elementals, -1);
        assertEquals(CARRIER_ID, elementals.getTransportId(), "Setup: the Elementals should be in the Atlas's arms");
    }

    @AfterEach
    void afterEach() {
        mockedServer.close();
    }

    @Test
    void editingCarriedUnitInLobbyRelinksTheCarrier() throws Exception {
        gameManager.handlePacket(CONNECTION_ID,
              new Packet(PacketCommand.ENTITY_UPDATE, clientCopy(game.getEntity(ELEMENTALS_ID))));

        assertCarrierHoldsGameUnit();
    }

    @Test
    void editingCarrierAndCarriedUnitTogetherRelinksTheCarrier() throws Exception {
        List<Entity> update = List.of(clientCopy(game.getEntity(ELEMENTALS_ID)),
              clientCopy(game.getEntity(CARRIER_ID)));
        gameManager.handlePacket(CONNECTION_ID, new Packet(PacketCommand.ENTITY_MULTI_UPDATE, update));

        assertCarrierHoldsGameUnit();
    }

    @Test
    void droppingAfterLobbyEditPlacesTheGameUnit() throws Exception {
        gameManager.handlePacket(CONNECTION_ID,
              new Packet(PacketCommand.ENTITY_UPDATE, clientCopy(game.getEntity(ELEMENTALS_ID))));
        game.setPhase(GamePhase.MOVEMENT);
        Entity carrier = game.getEntity(CARRIER_ID);
        carrier.setPosition(DROP_HEX);
        carrier.setDeployed(true);

        // The same calls the Drop Cargo move step makes
        ICarryable cargo = CarriedUnitRelinker.currentInstanceOf(game,
              carrier.getDistinctCarriedObjects().getFirst());
        carrier.dropCarriedObject(cargo, true);
        assertTrue(gameManager.unloadUnit(carrier, (Entity) cargo, DROP_HEX, 0, 0));

        Entity elementals = game.getEntity(ELEMENTALS_ID);
        assertEquals(Entity.NONE, elementals.getTransportId(), "The game's Elementals should no longer be carried");
        assertTrue(elementals.isDeployed(), "The game's Elementals should be deployed");
        assertEquals(DROP_HEX, elementals.getPosition(), "The game's Elementals should be in the drop hex");
    }

    @Test
    void dropFindsTheGameUnitEvenIfTheCarrierHoldsAnOldCopy() throws Exception {
        Entity staleCopy = game.getEntity(ELEMENTALS_ID);
        game.setEntity(ELEMENTALS_ID, clientCopy(staleCopy));

        ICarryable cargo = CarriedUnitRelinker.currentInstanceOf(game, staleCopy);

        assertNotSame(staleCopy, cargo);
        assertSame(game.getEntity(ELEMENTALS_ID), cargo);
    }

    private void assertCarrierHoldsGameUnit() {
        Entity carrier = game.getEntity(CARRIER_ID);
        Entity elementals = game.getEntity(ELEMENTALS_ID);
        assertSame(elementals, carrier.getDistinctCarriedObjects().getFirst(),
              "The Atlas's carried objects should hold the game's Elementals");
        for (Transporter transporter : carrier.getTransports()) {
            if (transporter instanceof ExternalCargo externalCargo) {
                for (ICarryable carryable : externalCargo.getCarryables()) {
                    assertSame(elementals, carryable,
                          transporter.getClass().getSimpleName() + " should hold the game's Elementals");
                }
            }
        }
    }

    private Entity addUnit(String fileName, int id) throws Exception {
        Entity unit = new MekFileParser(new File("testresources/megamek/common/units/" + fileName)).getEntity();
        unit.setId(id);
        unit.setGame(game);
        unit.setOwnerId(CONNECTION_ID);
        game.addEntity(unit);
        return unit;
    }

    /** A copy of the unit as a client would send it: serialized and read back. */
    private static Entity clientCopy(Entity unit) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(unit);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (Entity) in.readObject();
        }
    }
}

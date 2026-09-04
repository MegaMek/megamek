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

import static megamek.testUtilities.MMTestUtilities.getEntityForUnitTesting;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import megamek.client.ratgenerator.CarrierLoadingConfigurator;
import megamek.client.ratgenerator.ForceDescriptor;
import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.game.Game;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.units.Crew;
import megamek.common.units.Entity;
import megamek.server.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Drives a generated force through the lobby's Add to Game path: the tree is marked up as
 * {@code ForceGeneratorViewUi.addChosenUnits} does, handed to the server as an ENTITY_ADD packet
 * ({@code TWGameManager.receiveEntityAdd}), and the server's units are checked to see who is aboard whom.
 */
class ForceGeneratorCarrierLoadTest {

    private static final int CONNECTION_ID = 0;
    private static final String DROPSHIP = "Leopard (2537)";
    private static final String FIGHTER = "Cheetah F-11";
    private static final String MEK = "Atlas AS7-D";

    private Game serverGame;
    private TWGameManager gameManager;
    private Server server;
    private Player owner;

    @BeforeEach
    void setUp() throws Exception {
        serverGame = new Game();
        serverGame.setPhase(GamePhase.LOUNGE);
        owner = new Player(CONNECTION_ID, "Owner");
        owner.setTeam(1);
        serverGame.addPlayer(CONNECTION_ID, owner);
        gameManager = new TWGameManager();
        gameManager.setGame(serverGame);
        // Port 0 binds an ephemeral port; the server is only needed so that packet sends have a target
        server = new Server(null, 0, gameManager);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.die();
        }
    }

    @Test
    void fightersArriveAboardTheirShip() throws Exception {
        Entity leopard = load(DROPSHIP, true, "ship");
        Entity firstFighter = load(FIGHTER, true, "fighter-1");
        Entity secondFighter = load(FIGHTER, true, "fighter-2");
        Entity atlas = load(MEK, false, "mek");
        ForceDescriptor root = generatedForce(leopard, List.of(firstFighter, secondFighter), atlas);

        CarrierLoadingConfigurator.configure(root, entity -> true);
        gameManager.handlePacket(CONNECTION_ID,
              new Packet(PacketCommand.ENTITY_ADD,
                    new ArrayList<>(List.of(atlas, leopard, firstFighter, secondFighter))));

        Entity serverShip = byTag("ship");
        assertEquals(2, serverShip.getLoadedUnits().size(), "Both fighters are aboard the Leopard");
        assertEquals(serverShip.getId(), byTag("fighter-1").getTransportId());
        assertEquals(serverShip.getId(), byTag("fighter-2").getTransportId());
        assertEquals(Entity.NONE, byTag("mek").getTransportId(), "The Mek stays on the ground");
    }

    @Test
    void clientIdsThatCollideWithServerIdsAreTranslated() throws Exception {
        // Units already in the game hold the low ids the batch will be numbered with.
        for (int index = 0; index < 4; index++) {
            Entity present = load(MEK, false, "present-" + index);
            present.setId(serverGame.getNextEntityId());
            serverGame.addEntity(present);
        }
        Entity leopard = load(DROPSHIP, true, "ship");
        Entity fighter = load(FIGHTER, true, "fighter");
        ForceDescriptor root = generatedForce(leopard, List.of(fighter), null);

        CarrierLoadingConfigurator.configure(root, entity -> true);
        int shipClientId = leopard.getId();
        gameManager.handlePacket(CONNECTION_ID,
              new Packet(PacketCommand.ENTITY_ADD, new ArrayList<>(List.of(leopard, fighter))));

        Entity serverShip = byTag("ship");
        assertNotEquals(shipClientId, serverShip.getId(), "Precondition: the server had to renumber the ship");
        assertEquals(serverShip.getId(), byTag("fighter").getTransportId(),
              "The fighter follows the ship to its new id rather than whoever held the old one");
        assertEquals(1, serverShip.getLoadedUnits().size());
        assertEquals(Entity.NONE, byTag("present-" + (shipClientId - 1)).getTransportId(),
              "The unit that held the ship's client id carries nothing");
    }

    /**
     * The shape the generator produces: a Mek on the ground, and a Leopard under the transport branch with its
     * fighter complement attached beneath it.
     */
    private static ForceDescriptor generatedForce(Entity ship, List<Entity> fighters, Entity groundUnit)
          throws Exception {
        ForceDescriptor root = formation("Battalion");
        if (groundUnit != null) {
            ForceDescriptor lance = formation("Lance");
            lance.addSubForce(unit(groundUnit));
            root.addSubForce(lance);
        }
        ForceDescriptor transports = formation("Naval Units");
        ForceDescriptor shipNode = unit(ship);
        ForceDescriptor flight = formation("Flight 1");
        for (Entity fighter : fighters) {
            flight.addSubForce(unit(fighter));
        }
        shipNode.addAttached(flight);
        transports.addSubForce(shipNode);
        root.addAttached(transports);
        return root;
    }

    private Entity load(String unitName, boolean isBlk, String tag) {
        Entity entity = getEntityForUnitTesting(unitName, isBlk);
        assertNotNull(entity, unitName + " not found in the test data");
        entity.setCrew(new Crew(entity.defaultCrewType()));
        entity.getCrew().setName(tag, 0);
        entity.setOwner(owner);
        return entity;
    }

    private Entity byTag(String tag) {
        List<String> present = new ArrayList<>();
        for (Entity entity : serverGame.getEntitiesVector()) {
            if (tag.equals(entity.getCrew().getName(0))) {
                return entity;
            }
            present.add(entity.getShortNameRaw() + "/" + entity.getCrew().getName(0));
        }
        fail("No unit tagged " + tag + "; present: " + present);
        return null;
    }

    private static ForceDescriptor formation(String name) {
        ForceDescriptor descriptor = new ForceDescriptor();
        descriptor.setName(name);
        return descriptor;
    }

    /** {@code ForceDescriptor.entity} is only ever set by generation, so the test injects it. */
    private static ForceDescriptor unit(Entity entity) throws Exception {
        ForceDescriptor descriptor = new ForceDescriptor();
        descriptor.setName(entity.getShortName());
        descriptor.setElement(true);
        Field field = ForceDescriptor.class.getDeclaredField("entity");
        field.setAccessible(true);
        field.set(descriptor, entity);
        return descriptor;
    }
}

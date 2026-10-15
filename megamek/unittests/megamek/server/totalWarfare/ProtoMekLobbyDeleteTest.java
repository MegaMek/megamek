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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.loaders.MekFileParser;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.server.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers deleting a ProtoMek from the lobby when the player has more than one Point. The server moves a ProtoMek from
 * the last Point into the deleted one's Point, and used to crash looking it up. Drives the real packet path.
 */
class ProtoMekLobbyDeleteTest {

    private static final int PLAYER_CONNECTION = 0;
    private static final String TEST_UNIT = "Centaur";

    private Game game;
    private TWGameManager gameManager;
    private Server server;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() throws Exception {
        game = new Game();
        Player player = new Player(PLAYER_CONNECTION, "Player");
        player.setTeam(1);
        game.addPlayer(PLAYER_CONNECTION, player);

        gameManager = new TWGameManager();
        gameManager.setGame(game);
        // Port 0 binds an ephemeral port; the server is only needed so that packet sends have a target
        server = new Server(null, 0, gameManager);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.die();
        }
    }

    /** Adds ProtoMeks one at a time over the lobby's add packet, so the server numbers them into Points. */
    private List<Entity> addProtoMeks(int count) {
        List<Entity> added = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            Entity entity;
            try {
                entity = new MekFileParser(new File("testresources/data/mekfiles/" + TEST_UNIT + ".blk")).getEntity();
            } catch (Exception ex) {
                fail("Failed to load " + TEST_UNIT + ": " + ex.getMessage());
                return added;
            }
            entity.setGame(game);
            entity.setCrew(new Crew(CrewType.SINGLE));
            entity.setOwner(game.getPlayer(PLAYER_CONNECTION));
            gameManager.handlePacket(PLAYER_CONNECTION, new Packet(PacketCommand.ENTITY_ADD, List.of(entity)));
            added.add(game.getEntitiesVector().get(game.getEntitiesVector().size() - 1));
        }
        return added;
    }

    @Test
    void deletingFromAFullPointMovesTheLastPointIntoTheGap() {
        List<Entity> protoMeks = addProtoMeks(6);
        Entity firstOfPointOne = protoMeks.get(0);
        Entity aloneInPointTwo = protoMeks.get(5);
        assertEquals(0, firstOfPointOne.getUnitNumber(), "the first five form Point 1");
        assertEquals(1, aloneInPointTwo.getUnitNumber(), "the sixth starts Point 2");

        gameManager.handlePacket(PLAYER_CONNECTION,
              new Packet(PacketCommand.ENTITY_REMOVE, List.of(firstOfPointOne.getId())));

        assertNull(game.getEntity(firstOfPointOne.getId()), "the deleted ProtoMek must be gone");
        assertEquals(5, game.getEntitiesVector().size());
        assertEquals(0, aloneInPointTwo.getUnitNumber(), "the sixth ProtoMek moves into the deleted one's Point");
    }

    @Test
    void deletingTheOnlyMemberOfTheLastPointNeedsNoRenumbering() {
        List<Entity> protoMeks = addProtoMeks(6);
        Entity aloneInPointTwo = protoMeks.get(5);

        gameManager.handlePacket(PLAYER_CONNECTION,
              new Packet(PacketCommand.ENTITY_REMOVE, List.of(aloneInPointTwo.getId())));

        assertNull(game.getEntity(aloneInPointTwo.getId()));
        assertEquals(5, game.getEntitiesVector().size());
        for (Entity remaining : game.getEntitiesVector()) {
            assertEquals(0, remaining.getUnitNumber(), "the other five stay in Point 1");
        }
    }
}

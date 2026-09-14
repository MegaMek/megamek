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
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
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
 * Covers who the server lets a client add units for (GitHub issue #8860). The owner travels in the ENTITY_ADD payload
 * and used to be taken on trust, so a client could hand its units to anybody. Drives the real packet path.
 */
class EntityAddOwnershipTest {

    private static final int SENDER_CONNECTION = 0;
    private static final int OTHER_HUMAN_CONNECTION = 1;
    private static final int BOT_CONNECTION = 2;
    private static final String TEST_UNIT = "Atlas AS7-C";

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
        game.addPlayer(SENDER_CONNECTION, playerNamed(SENDER_CONNECTION, "Sender"));
        game.addPlayer(OTHER_HUMAN_CONNECTION, playerNamed(OTHER_HUMAN_CONNECTION, "Someone Else"));
        Player bot = playerNamed(BOT_CONNECTION, "Princess");
        bot.setBot(true);
        game.addPlayer(BOT_CONNECTION, bot);

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

    private static Player playerNamed(int id, String name) {
        Player player = new Player(id, name);
        player.setTeam(1);
        return player;
    }

    /** Sends one unit owned by the given player over the given connection, as the lobby does. */
    private void sendUnitFor(int ownerConnection, int overConnection) {
        Entity entity;
        try {
            entity = new MekFileParser(new File("testresources/data/mekfiles/" + TEST_UNIT + ".mtf")).getEntity();
        } catch (Exception ex) {
            fail("Failed to load " + TEST_UNIT + ": " + ex.getMessage());
            return;
        }
        entity.setGame(game);
        entity.setCrew(new Crew(CrewType.SINGLE));
        entity.setOwner(game.getPlayer(ownerConnection));
        gameManager.handlePacket(overConnection, new Packet(PacketCommand.ENTITY_ADD, List.of(entity)));
    }

    @Test
    void aPlayerMayAddUnitsForThemselves() {
        sendUnitFor(SENDER_CONNECTION, SENDER_CONNECTION);
        assertEquals(1, game.getEntitiesVector().size(), "the ordinary case must still work");
    }

    @Test
    void aPlayerMayNotAddUnitsForAnotherHuman() {
        sendUnitFor(OTHER_HUMAN_CONNECTION, SENDER_CONNECTION);
        assertEquals(0, game.getEntitiesVector().size(), "handing your units to another player must be refused");
    }

    @Test
    void aGameMasterMayAddUnitsForAnyone() {
        game.getPlayer(SENDER_CONNECTION).setGameMaster(true);
        sendUnitFor(OTHER_HUMAN_CONNECTION, SENDER_CONNECTION);
        assertEquals(1, game.getEntitiesVector().size(), "a gamemaster is meant to be able to set anybody up");
    }

    @Test
    void anyoneMayAddUnitsForABot() {
        // The known hole, kept deliberately: a unit for your own Princess arrives over your connection carrying the
        // bot's owner id, and the server holds no record of which human runs which bot.
        sendUnitFor(BOT_CONNECTION, SENDER_CONNECTION);
        assertEquals(1, game.getEntitiesVector().size(), "bots must still be stockable by the client running them");
    }

    @Test
    void aUnitOwnedByNobodyIsRefused() {
        Entity entity;
        try {
            entity = new MekFileParser(new File("testresources/data/mekfiles/" + TEST_UNIT + ".mtf")).getEntity();
        } catch (Exception ex) {
            fail("Failed to load " + TEST_UNIT + ": " + ex.getMessage());
            return;
        }
        entity.setGame(game);
        entity.setCrew(new Crew(CrewType.SINGLE));
        // A player object the server never saw, which is what an id for a departed or invented player looks like.
        entity.setOwner(playerNamed(99, "Ghost"));
        gameManager.handlePacket(SENDER_CONNECTION, new Packet(PacketCommand.ENTITY_ADD, List.of(entity)));
        assertEquals(0, game.getEntitiesVector().size(), "an owner who is not in the game must be refused");
    }

    /** Reassigning an existing unit, which is how a player actually gives one to their bot. */
    private void reassign(int unitOwnerConnection, int newOwnerConnection, int overConnection) {
        Entity entity = addedUnitFor(unitOwnerConnection);
        gameManager.handlePacket(overConnection,
              new Packet(PacketCommand.ENTITY_ASSIGN, List.of(entity), newOwnerConnection));
    }

    /** Puts one unit straight into the game owned by the given player, bypassing the add packet. */
    private Entity addedUnitFor(int ownerConnection) {
        Entity entity;
        try {
            entity = new MekFileParser(new File("testresources/data/mekfiles/" + TEST_UNIT + ".mtf")).getEntity();
        } catch (Exception ex) {
            fail("Failed to load " + TEST_UNIT + ": " + ex.getMessage());
            return null;
        }
        entity.setGame(game);
        entity.setCrew(new Crew(CrewType.SINGLE));
        entity.setOwner(game.getPlayer(ownerConnection));
        entity.setId(game.getNextEntityId());
        game.addEntity(entity);
        return entity;
    }

    private int ownerOfTheOnlyUnit() {
        return game.getEntitiesVector().get(0).getOwnerId();
    }

    @Test
    void aPlayerMayGiveTheirOwnUnitToTheirBot() {
        // The ordinary way to stock a Princess, and what the add-packet guard never sees.
        reassign(SENDER_CONNECTION, BOT_CONNECTION, SENDER_CONNECTION);
        assertEquals(BOT_CONNECTION, ownerOfTheOnlyUnit(), "giving your own unit to a bot must keep working");
    }

    @Test
    void aPlayerMayNotGiveAwayAUnitTheyDoNotOwn() {
        reassign(OTHER_HUMAN_CONNECTION, BOT_CONNECTION, SENDER_CONNECTION);
        assertEquals(OTHER_HUMAN_CONNECTION, ownerOfTheOnlyUnit(),
              "handing away another player's unit must be refused");
    }

    @Test
    void aPlayerMayNotPushTheirUnitOntoAnotherHuman() {
        reassign(SENDER_CONNECTION, OTHER_HUMAN_CONNECTION, SENDER_CONNECTION);
        assertEquals(SENDER_CONNECTION, ownerOfTheOnlyUnit(),
              "pushing your unit onto another player must be refused");
    }

    @Test
    void aGameMasterMayReassignBetweenOtherPlayers() {
        game.getPlayer(SENDER_CONNECTION).setGameMaster(true);
        reassign(OTHER_HUMAN_CONNECTION, BOT_CONNECTION, SENDER_CONNECTION);
        assertEquals(BOT_CONNECTION, ownerOfTheOnlyUnit(), "a gamemaster may move anybody's units");
    }
}

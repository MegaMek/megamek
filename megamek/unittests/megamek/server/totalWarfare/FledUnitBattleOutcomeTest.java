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
import megamek.common.interfaces.IEntityRemovalConditions;
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
 * Regression tests for issue #9282: a MekHQ unit that had been wrecked in an earlier battle, then recovered and
 * repaired, still carried that battle's result. When it fled the next battle the game could not lower "wreck" to
 * "fled", so MekHQ wrote it off and listed its pilot as missing. Drives the real lobby add packet.
 */
class FledUnitBattleOutcomeTest {

    private static final int PLAYER_CONNECTION = 0;
    private static final String TEST_UNIT = "Atlas AS7-C";
    /** The id the earlier battle's killer had; nothing in this game has it. */
    private static final int EARLIER_KILLER_ID = 34;

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

    private Entity loadUnit() {
        Entity entity = null;
        try {
            entity = new MekFileParser(new File("testresources/data/mekfiles/" + TEST_UNIT + ".mtf")).getEntity();
        } catch (Exception exception) {
            fail("Failed to load " + TEST_UNIT + ": " + exception.getMessage());
        }
        entity.setGame(game);
        entity.setCrew(new Crew(CrewType.SINGLE));
        entity.setOwner(game.getPlayer(PLAYER_CONNECTION));
        return entity;
    }

    /** A unit as MekHQ hands it over after it was wrecked, recovered and repaired. */
    private Entity unitWreckedInAnEarlierBattle() {
        Entity unit = loadUnit();
        unit.setRemovalCondition(IEntityRemovalConditions.REMOVE_SALVAGEABLE);
        Entity earlierKiller = loadUnit();
        earlierKiller.setId(EARLIER_KILLER_ID);
        earlierKiller.addKill(unit);
        return unit;
    }

    @Test
    void unitWreckedInAnEarlierBattleIsRecordedAsFledWhenItFlees() {
        gameManager.handlePacket(PLAYER_CONNECTION,
              new Packet(PacketCommand.ENTITY_ADD, List.of(unitWreckedInAnEarlierBattle())));
        Entity addedUnit = game.getEntitiesVector().get(0);

        // The last step of fleeing off the map
        game.removeEntity(addedUnit.getId(), IEntityRemovalConditions.REMOVE_IN_RETREAT);

        assertEquals(IEntityRemovalConditions.REMOVE_IN_RETREAT, addedUnit.getRemovalCondition(),
              "a unit that fled must be recorded as fled, not as the wreck it was in an earlier battle");
        assertEquals(Entity.NONE, addedUnit.getKillerId(),
              "the earlier battle's kill credit must not follow the unit into this one");
    }

    @Test
    void unitWreckedInThisBattleStaysAWreck() {
        gameManager.handlePacket(PLAYER_CONNECTION, new Packet(PacketCommand.ENTITY_ADD, List.of(loadUnit())));
        Entity addedUnit = game.getEntitiesVector().get(0);
        addedUnit.setRemovalCondition(IEntityRemovalConditions.REMOVE_SALVAGEABLE);

        game.removeEntity(addedUnit.getId(), IEntityRemovalConditions.REMOVE_IN_RETREAT);

        assertEquals(IEntityRemovalConditions.REMOVE_SALVAGEABLE, addedUnit.getRemovalCondition(),
              "within one battle a wreck must still outrank a later retreat");
    }
}

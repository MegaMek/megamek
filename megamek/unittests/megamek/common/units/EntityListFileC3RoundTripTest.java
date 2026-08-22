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
package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.loaders.MULParser;
import megamek.common.loaders.MekFileParser;
import megamek.common.util.C3Util;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression test for GitHub issue #8789: a MUL saved from the lobby must bring its C3 networks back when it is
 * loaded again. The load path is mirrored exactly - entities are parsed, added to a fresh game one at a time and
 * wired with {@link C3Util#wireC3(Game, Entity)} after each add, as the server does in receiveEntityAdd.
 */
public class EntityListFileC3RoundTripTest {

    private Game game;
    private static String lastParserWarning = "";

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = newGame();
    }

    private static Game newGame() {
        Game game = new Game();
        game.addPlayer(0, new Player(0, "Test Player"));
        return game;
    }

    /** Canonical C3 Master carrier (Atlas AS7-CM), loaded from testresources. */
    private static final String C3_MASTER_UNIT = "Atlas AS7-CM";
    /** Canonical C3 Slave carrier (Atlas AS7-C). */
    private static final String C3_SLAVE_UNIT = "Atlas AS7-C";
    /** Canonical C3i carrier (Crab CRB-30). */
    private static final String C3I_UNIT = "Crab CRB-30";

    /**
     * Loads a canonical unit file and adds it to the game. The crew name doubles as a tag so the unit can be found
     * again after the round trip, independent of the ids the reload assigns.
     */
    private Entity createUnit(Game targetGame, String unitName, String tag) {
        Entity entity;
        try {
            entity = new MekFileParser(new File("testresources/data/mekfiles/" + unitName + ".mtf")).getEntity();
        } catch (Exception ex) {
            fail("Failed to load " + unitName + ": " + ex.getMessage());
            return null;
        }
        entity.setGame(targetGame);
        entity.setId(targetGame.getNextEntityId());
        entity.setCrew(new Crew(CrewType.SINGLE));
        entity.getCrew().setName(tag, 0);
        entity.setOwner(targetGame.getPlayer(0));
        targetGame.addEntity(entity);
        return entity;
    }

    /** Saves the units to a temporary MUL (units embedded, so no unit cache is needed) and returns the file. */
    private static File toMul(List<Entity> entities) throws Exception {
        File file = File.createTempFile("c3-round-trip", ".mul");
        file.deleteOnExit();
        EntityListFile.saveTo(file, new ArrayList<>(entities), true);
        return file;
    }

    /** Parses the MUL and re-adds every unit to a fresh game the way the server does on load. */
    private static List<Entity> reload(File mul) throws Exception {
        MULParser parser = new MULParser(mul, null);
        lastParserWarning = parser.getWarningMessage();
        Vector<Entity> parsed = parser.getEntities();
        Game loadedGame = newGame();
        List<Entity> loaded = new ArrayList<>();
        for (Entity entity : parsed) {
            entity.setOwner(loadedGame.getPlayer(0));
            entity.setId(loadedGame.getNextEntityId());
            loadedGame.addEntity(entity);
            C3Util.wireC3(loadedGame, entity);
            loaded.add(entity);
        }
        return loaded;
    }

    private static Entity byTag(List<Entity> entities, String tag) {
        List<String> present = new ArrayList<>();
        for (Entity entity : entities) {
            if (tag.equals(entity.getCrew().getName(0))) {
                return entity;
            }
            present.add(entity.getShortNameRaw() + "/" + entity.getCrew().getName(0));
        }
        fail("No unit tagged " + tag + "; loaded: " + present + "; parser said: " + lastParserWarning);
        return null;
    }

    @Test
    void masterSlaveLanceSurvivesMulRoundTrip() throws Exception {
        Entity master = createUnit(game, C3_MASTER_UNIT, "master");
        Entity slaveOne = createUnit(game, C3_SLAVE_UNIT, "slave-1");
        Entity slaveTwo = createUnit(game, C3_SLAVE_UNIT, "slave-2");
        slaveOne.setC3Master(master, true);
        slaveTwo.setC3Master(master, true);
        assertTrue(slaveOne.onSameC3NetworkAs(slaveTwo), "precondition: lance is wired before saving");

        List<Entity> loaded = reload(toMul(List.of(master, slaveOne, slaveTwo)));

        Entity loadedMaster = byTag(loaded, "master");
        Entity loadedSlaveOne = byTag(loaded, "slave-1");
        Entity loadedSlaveTwo = byTag(loaded, "slave-2");
        assertNotNull(loadedSlaveOne.getC3Master(), "slave 1 lost its master after the MUL round trip");
        assertEquals(loadedMaster.getId(), loadedSlaveOne.getC3MasterId());
        assertEquals(loadedMaster.getId(), loadedSlaveTwo.getC3MasterId());
        assertTrue(loadedSlaveOne.onSameC3NetworkAs(loadedSlaveTwo));
    }

    @Test
    void slavesListedBeforeTheirMasterStillReconnect() throws Exception {
        Entity master = createUnit(game, C3_MASTER_UNIT, "master");
        Entity slave = createUnit(game, C3_SLAVE_UNIT, "slave");
        slave.setC3Master(master, true);

        // File order: slave first, so the master is not in the game yet when the slave is wired
        List<Entity> loaded = reload(toMul(List.of(slave, master)));

        assertEquals(byTag(loaded, "master").getId(), byTag(loaded, "slave").getC3MasterId());
    }

    @Test
    void c3iNetworkSurvivesMulRoundTrip() throws Exception {
        Entity first = createUnit(game, C3I_UNIT, "c3i-1");
        Entity second = createUnit(game, C3I_UNIT, "c3i-2");
        Entity third = createUnit(game, C3I_UNIT, "c3i-3");
        C3Util.joinNh(game, List.of(second, third), first.getId(), false);
        assertTrue(first.onSameC3NetworkAs(third), "precondition: C3i network is wired before saving");

        List<Entity> loaded = reload(toMul(List.of(first, second, third)));

        Entity loadedFirst = byTag(loaded, "c3i-1");
        assertTrue(loadedFirst.onSameC3NetworkAs(byTag(loaded, "c3i-2")), "c3i-1/c3i-2 link lost");
        assertTrue(loadedFirst.onSameC3NetworkAs(byTag(loaded, "c3i-3")), "c3i-1/c3i-3 link lost");
    }
}

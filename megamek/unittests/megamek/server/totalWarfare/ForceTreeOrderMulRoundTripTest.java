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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.force.Force;
import megamek.common.force.Forces;
import megamek.common.game.Game;
import megamek.common.loaders.MULParser;
import megamek.common.loaders.MekFileParser;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.EntityListFile;
import megamek.server.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Regression test for issue #9290: a unit list keeps no explicit order, so loading one rebuilds forces and units in
 * the order the units appear in the file. The lobby now saves in force tree order, so a battalion rearranged with the
 * up/down buttons reloads the way it was arranged. The file is loaded through the server's real lobby path
 * (ENTITY_ADD), and a save in plain unit order is the control that shows the arrangement is otherwise lost.
 */
class ForceTreeOrderMulRoundTripTest {

    private static final int CONNECTION_ID = 0;

    @TempDir
    Path tempDir;

    private Game sourceGame;
    private Game serverGame;
    private TWGameManager gameManager;
    private Server server;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() throws Exception {
        sourceGame = newGame();
        serverGame = newGame();
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

    private static Game newGame() {
        Game game = new Game();
        Player player = new Player(CONNECTION_ID, "Test Player");
        player.setTeam(1);
        game.addPlayer(CONNECTION_ID, player);
        return game;
    }

    private Entity createUnit(String tag) {
        Entity entity;
        try {
            entity = new MekFileParser(new File("testresources/data/mekfiles/Atlas AS7-D.mtf")).getEntity();
        } catch (Exception exception) {
            fail("Failed to load the test unit: " + exception.getMessage());
            return null;
        }
        entity.setGame(sourceGame);
        entity.setId(sourceGame.getNextEntityId());
        entity.setCrew(new Crew(CrewType.SINGLE));
        entity.getCrew().setName(tag, 0);
        entity.setOwner(sourceGame.getPlayer(CONNECTION_ID));
        sourceGame.addEntity(entity);
        return entity;
    }

    /**
     * Builds a battalion with two lances, adding the units lance by lance, then rearranges it with the lobby's up/down
     * moves: the second lance goes above the first, and the last unit of the first lance goes to its top.
     */
    private void buildRearrangedBattalion() {
        Player player = sourceGame.getPlayer(CONNECTION_ID);
        Forces forces = sourceGame.getForces();
        int battalionId = forces.addTopLevelForce(Force.createToplevelForce("Battalion", player), player);
        Force battalion = forces.getForce(battalionId);
        int firstLanceId = forces.addSubForce(Force.createSubforce("First Lance", battalion), battalion);
        int secondLanceId = forces.addSubForce(Force.createSubforce("Second Lance", battalion), battalion);

        Entity firstA = createUnit("first-a");
        Entity firstB = createUnit("first-b");
        Entity secondA = createUnit("second-a");
        Entity secondB = createUnit("second-b");
        forces.addEntity(firstA, firstLanceId);
        forces.addEntity(firstB, firstLanceId);
        forces.addEntity(secondA, secondLanceId);
        forces.addEntity(secondB, secondLanceId);

        assertTrue(!forces.moveUp(forces.getForce(secondLanceId)).isEmpty(), "precondition: lance moved up");
        assertTrue(!forces.moveUp(firstB).isEmpty(), "precondition: unit moved up");
    }

    /** Lists the server's tree as "force: units" lines, depth first, the way the lobby shows it. */
    private List<String> serverTree() {
        Forces forces = serverGame.getForces();
        List<String> lines = new ArrayList<>();
        for (Force topLevelForce : forces.getTopLevelForces()) {
            describe(forces, topLevelForce, lines);
        }
        return lines;
    }

    private void describe(Forces forces, Force force, List<String> lines) {
        List<String> unitTags = new ArrayList<>();
        for (int unitId : force.getEntities()) {
            unitTags.add(serverGame.getEntity(unitId).getCrew().getName(0));
        }
        lines.add(force.getName() + ": " + unitTags);
        for (int subForceId : force.getSubForces()) {
            describe(forces, forces.getForce(subForceId), lines);
        }
    }

    private void saveAndLoad(List<Entity> unitsInFileOrder) throws Exception {
        File file = tempDir.resolve("force-order.mul").toFile();
        EntityListFile.saveTo(file, new ArrayList<>(unitsInFileOrder), true);
        MULParser parser = new MULParser(file, null);
        List<Entity> loadedUnits = new ArrayList<>(parser.getEntities());
        for (Entity entity : loadedUnits) {
            entity.setOwner(serverGame.getPlayer(CONNECTION_ID));
        }
        gameManager.handlePacket(CONNECTION_ID, new Packet(PacketCommand.ENTITY_ADD, loadedUnits));
    }

    private static final List<String> ARRANGED_TREE = List.of(
          "Battalion: []",
          "Second Lance: [second-a, second-b]",
          "First Lance: [first-b, first-a]");

    @Test
    void savingInTreeOrderKeepsTheArrangement() throws Exception {
        buildRearrangedBattalion();

        saveAndLoad(sourceGame.getForces().sortedInTreeOrder(sourceGame.getEntitiesVector()));

        assertEquals(ARRANGED_TREE, serverTree());
    }

    @Test
    void savingInUnitOrderLosesTheArrangement() throws Exception {
        buildRearrangedBattalion();

        saveAndLoad(sourceGame.getEntitiesVector());

        assertNotEquals(ARRANGED_TREE, serverTree(), "control: plain unit order should not reproduce the tree");
    }
}

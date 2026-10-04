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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.game.InGameObject;
import megamek.common.loaders.MULParser;
import megamek.common.loaders.MekFileParser;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.EntityListFile;
import megamek.common.util.C3Util;
import megamek.server.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Regression test for GitHub issue #8789 (MUL files forget C3 networks). Drives the real lobby load path: the MUL is
 * parsed as ClientGUI.loadListFile does, handed to the server as an ENTITY_ADD packet (TWGameManager.receiveEntityAdd),
 * and the server's units are then pushed through a serialized copy into a client game via replaceUnits, which is
 * what the client does with the server's answer.
 */
class C3MulLoadTest {

    private static final int CONNECTION_ID = 0;
    private static final String C3_MASTER_UNIT = "Atlas AS7-CM";
    private static final String C3_SLAVE_UNIT = "Atlas AS7-C";
    private static final String C3I_UNIT = "Crab CRB-30";

    /** JUnit-managed scratch directory, private to this test run. */
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

    private Entity createUnit(String unitName, String tag) {
        Entity entity;
        try {
            entity = new MekFileParser(new File("testresources/data/mekfiles/" + unitName + ".mtf")).getEntity();
        } catch (Exception ex) {
            fail("Failed to load " + unitName + ": " + ex.getMessage());
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

    private File toMul(List<Entity> entities) throws Exception {
        File file = tempDir.resolve("c3-mul-load.mul").toFile();
        EntityListFile.saveTo(file, new ArrayList<>(entities), true);
        return file;
    }

    /** Parses the MUL as ClientGUI.loadListFile does and sends it to the server as the lobby would. */
    private void loadMulIntoServer(File mul) throws Exception {
        MULParser parser = new MULParser(mul, null);
        List<Entity> loadedUnits = new ArrayList<>(parser.getEntities());
        assertTrue(loadedUnits.size() > 1, "MUL should parse: " + parser.getWarningMessage());
        for (Entity entity : loadedUnits) {
            entity.setOwner(serverGame.getPlayer(CONNECTION_ID));
        }
        gameManager.handlePacket(CONNECTION_ID, new Packet(PacketCommand.ENTITY_ADD, loadedUnits));
    }

    /** Copies the server's units into a fresh client game the way the ENTITY_ADD answer does (serialized copies). */
    private static Game clientViewOf(Game serverGame) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(new ArrayList<>(serverGame.getEntitiesVector()));
        }
        Game clientGame = newGame();
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            @SuppressWarnings("unchecked")
            List<Entity> copies = (List<Entity>) in.readObject();
            List<InGameObject> units = new ArrayList<>(copies);
            clientGame.replaceUnits(units);
        }
        return clientGame;
    }

    private static Entity byTag(Game game, String tag) {
        List<String> present = new ArrayList<>();
        for (Entity entity : game.getEntitiesVector()) {
            if (tag.equals(entity.getCrew().getName(0))) {
                return entity;
            }
            present.add(entity.getShortNameRaw() + "/" + entity.getCrew().getName(0));
        }
        fail("No unit tagged " + tag + "; present: " + present);
        return null;
    }

    @Test
    void lanceSurvivesLobbyMulLoadOnServerAndClient() throws Exception {
        Entity master = createUnit(C3_MASTER_UNIT, "master");
        Entity slaveOne = createUnit(C3_SLAVE_UNIT, "slave-1");
        Entity slaveTwo = createUnit(C3_SLAVE_UNIT, "slave-2");
        C3Util.connect(sourceGame, new ArrayList<>(List.of(slaveOne, slaveTwo)), master.getId(), true);
        assertTrue(slaveOne.onSameC3NetworkAs(slaveTwo), "precondition: lance is wired before saving");

        loadMulIntoServer(toMul(List.of(slaveOne, master, slaveTwo)));

        Entity serverMaster = byTag(serverGame, "master");
        assertEquals(serverMaster.getId(), byTag(serverGame, "slave-1").getC3MasterId(), "server: slave 1");
        assertEquals(serverMaster.getId(), byTag(serverGame, "slave-2").getC3MasterId(), "server: slave 2");

        Game clientGame = clientViewOf(serverGame);
        Entity clientMaster = byTag(clientGame, "master");
        assertEquals(clientMaster.getId(), byTag(clientGame, "slave-1").getC3MasterId(), "client: slave 1");
        assertEquals(clientMaster.getId(), byTag(clientGame, "slave-2").getC3MasterId(), "client: slave 2");
        assertTrue(byTag(clientGame, "slave-1").onSameC3NetworkAs(byTag(clientGame, "slave-2")));
    }

    @Test
    void companyNetworkSurvivesLobbyMulLoad() throws Exception {
        Entity company = createUnit(C3_MASTER_UNIT, "company");
        Entity lance = createUnit(C3_MASTER_UNIT, "lance");
        Entity slave = createUnit(C3_SLAVE_UNIT, "slave");
        // Built the way the lobby does it: designate the company commander, then plain "Connect to" joins
        // (disconnectFirst=false); the true form is the "form lance" action and tears existing links down.
        company.setC3Master(company, true);
        C3Util.connect(sourceGame, new ArrayList<>(List.of(lance)), company.getId(), false);
        assertEquals(company.getId(), lance.getC3MasterId(), "precondition: lance master linked to company");
        C3Util.connect(sourceGame, new ArrayList<>(List.of(slave)), lance.getId(), false);
        assertEquals(lance.getId(), slave.getC3MasterId(), "precondition: slave linked to lance master");
        assertEquals(company.getC3NetId(), lance.getC3NetId(), "precondition: lance shares the company net id");
        assertEquals(lance.getC3NetId(), slave.getC3NetId(), "precondition: slave shares the lance net id");
        assertTrue(company.onSameC3NetworkAs(slave), "precondition: company network is wired before saving");

        loadMulIntoServer(toMul(List.of(slave, lance, company)));

        Game clientGame = clientViewOf(serverGame);
        Entity clientCompany = byTag(clientGame, "company");
        Entity clientLance = byTag(clientGame, "lance");
        assertTrue(clientCompany.isC3CompanyCommander(), "company commander flag lost");
        assertEquals(clientCompany.getId(), clientLance.getC3MasterId(), "lance master lost its company master");
        assertEquals(clientLance.getId(), byTag(clientGame, "slave").getC3MasterId(), "slave lost its lance master");
        assertTrue(clientCompany.onSameC3NetworkAs(byTag(clientGame, "slave")));
    }

    @Test
    void c3iNetworkSurvivesLobbyMulLoad() throws Exception {
        Entity first = createUnit(C3I_UNIT, "c3i-1");
        Entity second = createUnit(C3I_UNIT, "c3i-2");
        Entity third = createUnit(C3I_UNIT, "c3i-3");
        C3Util.joinNh(sourceGame, List.of(second, third), first.getId(), false);
        assertTrue(first.onSameC3NetworkAs(third), "precondition: C3i network is wired before saving");

        loadMulIntoServer(toMul(List.of(first, second, third)));

        Game clientGame = clientViewOf(serverGame);
        Entity clientFirst = byTag(clientGame, "c3i-1");
        assertTrue(clientFirst.onSameC3NetworkAs(byTag(clientGame, "c3i-2")), "c3i-1/c3i-2 link lost");
        assertTrue(clientFirst.onSameC3NetworkAs(byTag(clientGame, "c3i-3")), "c3i-1/c3i-3 link lost");
    }
}

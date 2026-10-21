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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.force.Force;
import megamek.common.force.Forces;
import megamek.common.game.Game;
import megamek.common.loaders.MekFileParser;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.InvalidPacketDataException;
import megamek.common.net.packets.Packet;
import megamek.common.units.Entity;
import megamek.server.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for issue #9290. Lobby force packets were only read when the forces arrived in a HashSet, so a
 * rename or a drag-and-drop attach, which the lobby sends as a list, was silently dropped by the server. Each case is
 * sent through the server's packet handling both as a list (what the lobby sends) and as a set, so a list test cannot
 * pass without the server acting at all.
 */
class LobbyForcePacketTest {

    private static final int CONNECTION_ID = 0;

    private Game serverGame;
    private TWGameManager gameManager;
    private Server server;
    private int alphaId;
    private int bravoId;
    private int lanceId;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() throws Exception {
        serverGame = new Game();
        Player player = new Player(CONNECTION_ID, "Test Player");
        player.setTeam(1);
        serverGame.addPlayer(CONNECTION_ID, player);
        gameManager = new TWGameManager();
        gameManager.setGame(serverGame);
        // Port 0 binds an ephemeral port; the server is only needed so that packet sends have a target
        server = new Server(null, 0, gameManager);

        Forces forces = serverGame.getForces();
        alphaId = forces.addTopLevelForce(Force.createToplevelForce("Alpha", player), player);
        bravoId = forces.addTopLevelForce(Force.createToplevelForce("Bravo", player), player);
        lanceId = forces.addSubForce(Force.createSubforce("Lance", forces.getForce(alphaId)),
              forces.getForce(alphaId));
        Entity unit = createUnit(player);
        forces.addEntity(unit, lanceId);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.die();
        }
    }

    private Entity createUnit(Player owner) {
        Entity entity;
        try {
            entity = new MekFileParser(new File("testresources/data/mekfiles/Atlas AS7-D.mtf")).getEntity();
        } catch (Exception exception) {
            fail("Failed to load the test unit: " + exception.getMessage());
            return null;
        }
        entity.setOwner(owner);
        serverGame.addEntity(entity);
        return entity;
    }

    /** Renames the lance on a copy of the forces, as LobbyActions.forceRename does on the client's copy. */
    private Force renamedLanceCopy(String newName) {
        Forces clientForces = serverGame.getForces().clone();
        clientForces.renameForce(newName, lanceId);
        return clientForces.getForce(lanceId);
    }

    private String serverLanceName() {
        return serverGame.getForces().getForce(lanceId).getName();
    }

    private void sendRename(Collection<Force> changedForces) {
        gameManager.handlePacket(CONNECTION_ID,
              new Packet(PacketCommand.FORCE_UPDATE, changedForces, new ArrayList<Entity>()));
    }

    private void sendAttach(Collection<Force> forcesToAttach, int newParentId) {
        gameManager.handlePacket(CONNECTION_ID, new Packet(PacketCommand.FORCE_PARENT, forcesToAttach, newParentId));
    }

    @Test
    void renameSentAsListReachesTheServer() {
        sendRename(new ArrayList<>(List.of(renamedLanceCopy("Command Lance"))));

        assertEquals("Command Lance", serverLanceName());
    }

    @Test
    void renameSentAsSetReachesTheServer() {
        sendRename(new HashSet<>(List.of(renamedLanceCopy("Command Lance"))));

        assertEquals("Command Lance", serverLanceName());
    }

    @Test
    void attachSentAsListReachesTheServer() {
        Force lance = serverGame.getForces().getForce(lanceId);

        sendAttach(new ArrayList<>(List.of(lance)), bravoId);

        assertEquals(bravoId, serverGame.getForces().getForce(lanceId).getParentId());
    }

    @Test
    void attachSentAsSetReachesTheServer() {
        Force lance = serverGame.getForces().getForce(lanceId);

        sendAttach(new HashSet<>(List.of(lance)), bravoId);

        assertEquals(bravoId, serverGame.getForces().getForce(lanceId).getParentId());
    }

    @Test
    void forceListIsReadFromAnyCollection() throws InvalidPacketDataException {
        // The server echoes a received update back out as a list, so clients must be able to read one too
        Force lance = serverGame.getForces().getForce(lanceId);
        Packet listPacket = new Packet(PacketCommand.FORCE_UPDATE, new ArrayList<>(List.of(lance)), List.of());
        Packet setPacket = new Packet(PacketCommand.FORCE_UPDATE, new HashSet<>(List.of(lance)), List.of());

        assertEquals(List.of(lance), listPacket.getForceList(0));
        assertEquals(List.of(lance), setPacket.getForceList(0));
    }
}

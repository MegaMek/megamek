/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
 * MechWarrior Copyright Microsoft Corporation. Megamek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.List;

import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.interfaces.IEntityRemovalConditions;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.util.C3Util;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for Nova CEWS network management and UUID persistence.
 *
 * TT Rules (IO: Alternate Eras p.60):
 * - Nova CEWS can link up to 2 other units (3 units total per network)
 * - Units must declare connection in End Phase
 * - Link becomes active next turn
 * - Operates per C3i rules
 *
 * These tests focus on network persistence, UUID management, and lobby→game transition.
 */
public class NovaCEWSNetworkTest {

    private Game game;
    private Entity entity1;
    private Entity entity2;
    private Entity entity3;
    private Entity entity4;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();

        // CRITICAL: Add a player to the game (entities need owners for BV calculation)
        game.addPlayer(0, new megamek.common.Player(0, "Test Player"));

        // Create test entities with Nova CEWS
        entity1 = createNovaCEWSEntity(1, "Unit 1");
        entity2 = createNovaCEWSEntity(2, "Unit 2");
        entity3 = createNovaCEWSEntity(3, "Unit 3");
        entity4 = createNovaCEWSEntity(4, "Unit 4");

        // Add entities to game
        game.addEntity(entity1);
        game.addEntity(entity2);
        game.addEntity(entity3);
        game.addEntity(entity4);
    }

    /**
     * Helper method to create an entity with Nova CEWS equipment.
     */
    private Entity createNovaCEWSEntity(int id, String name) {
        Entity entity = new BipedMek();
        entity.setGame(game);  // CRITICAL: Must set game before adding equipment
        entity.setId(id);
        entity.setChassis(name);
        entity.setModel("");

        // CRITICAL FIX: Initialize crew to avoid NullPointerException when adding to game
        Crew crew = new Crew(CrewType.SINGLE);
        entity.setCrew(crew);

        // CRITICAL FIX: Set owner (required for BV calculation in getExtraC3BV)
        entity.setOwner(game.getPlayer(0));

        // Set basic properties required by game
        entity.setWeight(50.0);
        entity.setOriginalWalkMP(5);

        // Add Nova CEWS equipment
        try {
            EquipmentType novaCEWS = EquipmentType.get("NovaCEWS");
            Mounted<?> novaMounted = entity.addEquipment(novaCEWS, Entity.LOC_NONE);
            novaMounted.setMode("ECM"); // Default mode
        } catch (Exception e) {
            fail("Failed to add Nova CEWS equipment: " + e.getMessage());
        }

        return entity;
    }

    /**
     * Test that a 2-unit network can be created successfully.
     */
    @Test
    void testCreate2UnitNetwork() throws Exception {
        List<Entity> entities = Arrays.asList(entity1, entity2);
        C3Util.joinNh(game, entities, entity1.getId(), true);

        // Verify both entities have the same network ID
        assertEquals(entity1.getC3NetId(), entity2.getC3NetId(),
            "Both units should have the same network ID");

        // Verify UUID cross-references
        String entity2UUID = entity2.getC3UUIDAsString();
        assertEquals(entity2UUID, entity1.getNC3NextUUIDAsString(0),
            "Entity 1 should have Entity 2's UUID in position 0");

        String entity1UUID = entity1.getC3UUIDAsString();
        assertEquals(entity1UUID, entity2.getNC3NextUUIDAsString(0),
            "Entity 2 should have Entity 1's UUID in position 0");
    }

    /**
     * Test that a 3-unit network can be created successfully (maximum size).
     */
    @Test
    void testCreate3UnitNetwork() throws Exception {
        List<Entity> entities = Arrays.asList(entity1, entity2, entity3);
        C3Util.joinNh(game, entities, entity1.getId(), true);

        // Verify all entities have the same network ID
        String networkId = entity1.getC3NetId();
        assertEquals(networkId, entity2.getC3NetId(),
            "Entity 2 should have the same network ID as Entity 1");
        assertEquals(networkId, entity3.getC3NetId(),
            "Entity 3 should have the same network ID as Entity 1");

        // Verify Entity 1 has both partners' UUIDs
        String entity2UUID = entity2.getC3UUIDAsString();
        String entity3UUID = entity3.getC3UUIDAsString();

        assertTrue(
            entity2UUID.equals(entity1.getNC3NextUUIDAsString(0)) ||
            entity2UUID.equals(entity1.getNC3NextUUIDAsString(1)),
            "Entity 1 should have Entity 2's UUID in its array"
        );

        assertTrue(
            entity3UUID.equals(entity1.getNC3NextUUIDAsString(0)) ||
            entity3UUID.equals(entity1.getNC3NextUUIDAsString(1)),
            "Entity 1 should have Entity 3's UUID in its array"
        );
    }

    /**
     * Test that wireC3() correctly reconstructs network IDs from UUID arrays.
     * This simulates what happens when entities are deserialized from the lobby.
     */
    @Test
    void testWireC3ReconstructsNetworkFromUUIDs() throws Exception {
        // Step 1: Create network in lobby (via joinNh)
        List<Entity> entities = Arrays.asList(entity1, entity2, entity3);
        C3Util.joinNh(game, entities, entity1.getId(), true);

        String originalNetworkId = entity1.getC3NetId();

        // Step 2: Simulate serialization/deserialization by clearing network IDs
        // (but keeping UUID arrays intact, as happens in actual serialization)
        entity1.setC3NetIdSelf(); // Resets to unique ID
        entity2.setC3NetIdSelf();
        entity3.setC3NetIdSelf();

        // Verify each entity now has its own unique network ID
        assertNotEquals(entity1.getC3NetId(), entity2.getC3NetId(),
            "Entity 1 and Entity 2 should have different network IDs after setC3NetIdSelf()");
        assertNotEquals(entity1.getC3NetId(), entity3.getC3NetId(),
            "Entity 1 and Entity 3 should have different network IDs after setC3NetIdSelf()");

        // Step 3: Call wireC3() to reconstruct network (simulates client-side processing)
        C3Util.wireC3(game, entity1);
        C3Util.wireC3(game, entity2);
        C3Util.wireC3(game, entity3);

        // Step 4: Verify network IDs are reconstructed
        String reconstructedNetworkId = entity1.getC3NetId();
        assertEquals(reconstructedNetworkId, entity2.getC3NetId(),
            "Entity 2 should have the same network ID after wireC3()");
        assertEquals(reconstructedNetworkId, entity3.getC3NetId(),
            "Entity 3 should have the same network ID after wireC3()");
    }

    /**
     * Test that joinNh() clears old UUID entries before assigning new ones.
     * This prevents stale UUIDs from previous network configurations.
     */
    @Test
    void testJoinNhClearsOldUUIDs() throws Exception {
        // Create initial 2-unit network
        List<Entity> initialNetwork = Arrays.asList(entity1, entity2);
        C3Util.joinNh(game, initialNetwork, entity1.getId(), true);

        // Verify initial network
        assertNotNull(entity1.getNC3NextUUIDAsString(0),
            "Entity 1 should have a UUID at position 0");
        assertNull(entity1.getNC3NextUUIDAsString(1),
            "Entity 1 should have null at position 1 (only 1 partner)");

        // Disconnect entity2 from the network before reconfiguring
        C3Util.disconnectFromNetwork(game, Arrays.asList(entity2));

        // Reconfigure to 3-unit network (entity1, entity3, entity4)
        List<Entity> newNetwork = Arrays.asList(entity1, entity3, entity4);
        C3Util.joinNh(game, newNetwork, entity1.getId(), true);

        // Verify old UUID is cleared and new UUIDs are present
        String uuid3 = entity3.getC3UUIDAsString();
        String uuid4 = entity4.getC3UUIDAsString();

        boolean hasUuid3 = uuid3.equals(entity1.getNC3NextUUIDAsString(0)) ||
                          uuid3.equals(entity1.getNC3NextUUIDAsString(1));
        boolean hasUuid4 = uuid4.equals(entity1.getNC3NextUUIDAsString(0)) ||
                          uuid4.equals(entity1.getNC3NextUUIDAsString(1));

        assertTrue(hasUuid3, "Entity 1 should have Entity 3's UUID");
        assertTrue(hasUuid4, "Entity 1 should have Entity 4's UUID");

        // Verify old UUID from entity2 is NOT present
        String oldUuid2 = entity2.getC3UUIDAsString();
        assertFalse(
            oldUuid2.equals(entity1.getNC3NextUUIDAsString(0)) ||
            oldUuid2.equals(entity1.getNC3NextUUIDAsString(1)),
            "Entity 1 should NOT have old Entity 2 UUID after reconfiguration"
        );
    }

    /**
     * Test that performDisconnect() properly clears UUID arrays.
     */
    @Test
    void testPerformDisconnectClearsUUIDs() throws Exception {
        // Create network
        List<Entity> entities = Arrays.asList(entity1, entity2);
        C3Util.joinNh(game, entities, entity1.getId(), true);

        // Verify UUIDs are present
        assertNotNull(entity1.getNC3NextUUIDAsString(0),
            "Entity 1 should have a UUID before disconnect");

        // Disconnect
        C3Util.disconnectFromNetwork(game, Arrays.asList(entity1));

        // Verify UUID array is cleared
        assertNull(entity1.getNC3NextUUIDAsString(0),
            "Entity 1 UUID array should be cleared after disconnect");
        assertNull(entity1.getNC3NextUUIDAsString(1),
            "Entity 1 UUID array should be cleared after disconnect");
    }

    /**
     * Test that entities can be unlinked and then re-linked to a different network.
     */
    @Test
    void testUnlinkAndRelinkToNewNetwork() throws Exception {
        // Create initial network: entity1 + entity2
        C3Util.joinNh(game, Arrays.asList(entity1, entity2), entity1.getId(), true);
        String network1Id = entity1.getC3NetId();

        // Create second network: entity3 + entity4
        C3Util.joinNh(game, Arrays.asList(entity3, entity4), entity3.getId(), true);
        String network2Id = entity3.getC3NetId();

        // Verify separate networks
        assertNotEquals(network1Id, network2Id,
            "Networks should have different IDs");

        // Disconnect entity2 from network1
        C3Util.disconnectFromNetwork(game, Arrays.asList(entity2));

        // Re-link entity2 to network2
        C3Util.joinNh(game, Arrays.asList(entity3, entity4, entity2), entity3.getId(), true);

        // Verify entity2 is now on network2
        assertEquals(network2Id, entity2.getC3NetId(),
            "Entity 2 should now be on network 2");

        // Verify entity2 has UUIDs from network2 members
        String uuid3 = entity3.getC3UUIDAsString();
        String uuid4 = entity4.getC3UUIDAsString();

        boolean hasUuid3 = uuid3.equals(entity2.getNC3NextUUIDAsString(0)) ||
                          uuid3.equals(entity2.getNC3NextUUIDAsString(1));
        boolean hasUuid4 = uuid4.equals(entity2.getNC3NextUUIDAsString(0)) ||
                          uuid4.equals(entity2.getNC3NextUUIDAsString(1));

        assertTrue(hasUuid3 || hasUuid4,
            "Entity 2 should have UUIDs from its new network partners");
    }

    /**
     * Test that all slots in UUID array are properly managed.
     * Entity should have exactly N-1 UUIDs for N-entity network.
     */
    @Test
    void testUUIDArraySlotManagement() throws Exception {
        // 3-unit network: entity should have 2 UUIDs
        C3Util.joinNh(game, Arrays.asList(entity1, entity2, entity3), entity1.getId(), true);

        int uuidCount = 0;
        for (int i = 0; i < Entity.MAX_C3i_NODES; i++) {
            if (entity1.getNC3NextUUIDAsString(i) != null) {
                uuidCount++;
            }
        }

        assertEquals(2, uuidCount,
            "Entity 1 should have exactly 2 UUIDs (for 2 partners in 3-unit network)");
    }

    /**
     * Test that network remains intact after one member is removed from game.
     */
    @Test
    void testNetworkPersistsAfterMemberRemoval() throws Exception {
        // Create 3-unit network
        C3Util.joinNh(game, Arrays.asList(entity1, entity2, entity3), entity1.getId(), true);
        String networkId = entity1.getC3NetId();

        // Remove entity3 from game
        game.removeEntity(entity3.getId(), IEntityRemovalConditions.REMOVE_SALVAGEABLE);

        // Remaining entities should still have same network ID
        assertEquals(networkId, entity1.getC3NetId(),
            "Entity 1 should retain network ID after member removal");
        assertEquals(networkId, entity2.getC3NetId(),
            "Entity 2 should retain network ID after member removal");
    }
}

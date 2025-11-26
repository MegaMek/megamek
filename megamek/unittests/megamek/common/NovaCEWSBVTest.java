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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for Nova CEWS BV calculation via Entity.getExtraC3BV().
 *
 * TT Rules (IO: Alternate Eras p.183):
 * - Nova CEWS provides 5% BV bonus for all friendly units with Nova CEWS
 * - Bonus only applies if 2+ units with Nova CEWS are present
 * - Maximum bonus capped at 35% of unit's base BV
 *
 * Creates entities programmatically with Nova CEWS equipment.
 */
public class NovaCEWSBVTest {

    private Game game;
    private int nextEntityId = 1;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        game.addPlayer(0, new Player(0, "Test Player"));
        nextEntityId = 1;
    }

    /**
     * Helper method to create an entity with Nova CEWS equipment.
     */
    private Entity createNovaCEWSEntity() {
        Entity entity = new BipedMek();
        entity.setGame(game);
        entity.setId(nextEntityId++);
        entity.setChassis("Test Mek");
        entity.setModel("Nova");

        // Initialize crew to avoid NullPointerException
        Crew crew = new Crew(CrewType.SINGLE);
        entity.setCrew(crew);

        // Set owner (required for BV calculation)
        entity.setOwner(game.getPlayer(0));

        // Set basic properties
        entity.setWeight(50.0);
        entity.setOriginalWalkMP(5);

        // Add Nova CEWS equipment
        try {
            EquipmentType novaCEWS = EquipmentType.get("NovaCEWS");
            Mounted<?> novaMounted = entity.addEquipment(novaCEWS, Entity.LOC_NONE);
            novaMounted.setMode("ECM");
        } catch (Exception e) {
            fail("Failed to add Nova CEWS equipment: " + e.getMessage());
        }

        return entity;
    }

    /**
     * Helper method to create an entity WITHOUT Nova CEWS equipment.
     */
    private Entity createStandardEntity() {
        Entity entity = new BipedMek();
        entity.setGame(game);
        entity.setId(nextEntityId++);
        entity.setChassis("Test Mek");
        entity.setModel("Standard");

        // Initialize crew to avoid NullPointerException
        Crew crew = new Crew(CrewType.SINGLE);
        entity.setCrew(crew);

        // Set owner (required for BV calculation)
        entity.setOwner(game.getPlayer(0));

        // Set basic properties
        entity.setWeight(50.0);
        entity.setOriginalWalkMP(5);

        return entity;
    }

    /**
     * Verify that the test unit has Nova CEWS equipment.
     */
    @Test
    void testEntityHasNovaCEWS() {
        Entity entity = createNovaCEWSEntity();
        game.addEntity(entity);

        assertTrue(entity.hasNovaCEWS(), "Entity with Nova CEWS equipment should return true for hasNovaCEWS()");
    }

    /**
     * Single unit with Nova CEWS should get no bonus (requires 2+ units).
     */
    @Test
    void testGetExtraC3BV_NoBonus_SingleUnit() {
        Entity entity = createNovaCEWSEntity();
        game.addEntity(entity);

        int baseBV = entity.calculateBattleValue(true, true);
        int extraBV = entity.getExtraC3BV(baseBV);

        assertEquals(0, extraBV, "Single Nova CEWS unit should get no bonus");
    }

    /**
     * Two units with Nova CEWS should get 5% of total force BV as bonus.
     */
    @Test
    void testGetExtraC3BV_TwoUnitNetwork() {
        Entity entity1 = createNovaCEWSEntity();
        Entity entity2 = createNovaCEWSEntity();
        game.addEntity(entity1);
        game.addEntity(entity2);

        int baseBV1 = entity1.calculateBattleValue(true, true);
        int baseBV2 = entity2.calculateBattleValue(true, true);

        // Expected: (baseBV1 + baseBV2) * 0.05
        int expectedBonus = (int) Math.round((baseBV1 + baseBV2) * 0.05);

        int actualBonus = entity1.getExtraC3BV(baseBV1);

        assertEquals(expectedBonus, actualBonus,
              "Nova CEWS bonus should be 5% of total friendly Nova CEWS force BV");
    }

    /**
     * Three units with Nova CEWS should get 5% of total force BV as bonus.
     */
    @Test
    void testGetExtraC3BV_ThreeUnitNetwork() {
        Entity entity1 = createNovaCEWSEntity();
        Entity entity2 = createNovaCEWSEntity();
        Entity entity3 = createNovaCEWSEntity();
        game.addEntity(entity1);
        game.addEntity(entity2);
        game.addEntity(entity3);

        int baseBV1 = entity1.calculateBattleValue(true, true);
        int baseBV2 = entity2.calculateBattleValue(true, true);
        int baseBV3 = entity3.calculateBattleValue(true, true);

        // Expected: (baseBV1 + baseBV2 + baseBV3) * 0.05
        int totalForceBV = baseBV1 + baseBV2 + baseBV3;
        int expectedBonus = (int) Math.round(totalForceBV * 0.05);

        int actualBonus = entity1.getExtraC3BV(baseBV1);

        assertEquals(expectedBonus, actualBonus,
              "Nova CEWS bonus should be 5% of total friendly Nova CEWS force BV");
    }

    /**
     * Bonus should be capped at 35% of unit's base BV when total force BV is high.
     * With enough units, raw 5% bonus would exceed 35% cap.
     */
    @Test
    void testGetExtraC3BV_CappedAt35Percent() {
        Entity entity1 = createNovaCEWSEntity();
        game.addEntity(entity1);

        int baseBV = entity1.calculateBattleValue(true, true);

        // Add enough units to exceed 35% cap
        // 35% cap means we need totalForceBV * 0.05 > baseBV * 0.35
        // Which means totalForceBV > baseBV * 7
        // So we need 8+ units total to exceed the cap
        for (int i = 0; i < 8; i++) {
            Entity entity = createNovaCEWSEntity();
            game.addEntity(entity);
        }

        int maxBonus = (int) Math.round(baseBV * 0.35);

        int actualBonus = entity1.getExtraC3BV(baseBV);

        assertEquals(maxBonus, actualBonus,
              "Nova CEWS bonus should be capped at 35% of base BV (" + maxBonus + ")");
    }

    /**
     * Entity without Nova CEWS should get no C3 bonus from Nova CEWS units.
     */
    @Test
    void testGetExtraC3BV_NoBonus_WithoutNovaCEWS() {
        // Create an entity without Nova CEWS
        Entity standardEntity = createStandardEntity();
        game.addEntity(standardEntity);

        // Add some entities with Nova CEWS
        Entity novaEntity1 = createNovaCEWSEntity();
        Entity novaEntity2 = createNovaCEWSEntity();
        game.addEntity(novaEntity1);
        game.addEntity(novaEntity2);

        int baseBV = standardEntity.calculateBattleValue(true, true);
        int extraBV = standardEntity.getExtraC3BV(baseBV);

        assertEquals(0, extraBV,
              "Entity without Nova CEWS should get no bonus from Nova CEWS units");
    }
}

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

import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.utils.EntityLoader;
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
 * Uses Pariah (Septicemia) A-Z (Base BV: 2,388) which has Nova CEWS built-in.
 */
public class NovaCEWSBVTest {

    private static final String PARIAH_FILE = "Pariah (Septicemia) A-Z.mtf";

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
     * Helper method to load the Pariah (Septicemia) A-Z unit and configure it for the game.
     */
    private Entity loadPariah() {
        Entity entity = EntityLoader.loadFromFile(PARIAH_FILE);
        entity.setGame(game);
        entity.setOwner(game.getPlayer(0));
        entity.setId(nextEntityId++);
        return entity;
    }

    /**
     * Verify that the test unit has Nova CEWS and expected base BV.
     */
    @Test
    void testPariahHasNovaCEWS() {
        Entity pariah = loadPariah();
        game.addEntity(pariah);

        assertTrue(pariah.hasNovaCEWS(), "Pariah (Septicemia) A-Z should have Nova CEWS");

        int baseBV = pariah.calculateBattleValue(true, true);
        assertEquals(2388, baseBV, "Pariah base BV should be 2388");
    }

    /**
     * Single unit with Nova CEWS should get no bonus (requires 2+ units).
     */
    @Test
    void testGetExtraC3BV_NoBonus_SingleUnit() {
        Entity pariah = loadPariah();
        game.addEntity(pariah);

        int baseBV = pariah.calculateBattleValue(true, true);
        int extraBV = pariah.getExtraC3BV(baseBV);

        assertEquals(0, extraBV, "Single Nova CEWS unit should get no bonus");
    }

    /**
     * Two units with Nova CEWS should get 5% of total force BV as bonus.
     */
    @Test
    void testGetExtraC3BV_TwoUnitNetwork() {
        Entity pariah1 = loadPariah();
        Entity pariah2 = loadPariah();
        game.addEntity(pariah1);
        game.addEntity(pariah2);

        int baseBV1 = pariah1.calculateBattleValue(true, true);
        int baseBV2 = pariah2.calculateBattleValue(true, true);

        // Expected: (baseBV1 + baseBV2) * 0.05
        int expectedBonus = (int) Math.round((baseBV1 + baseBV2) * 0.05);

        int actualBonus = pariah1.getExtraC3BV(baseBV1);

        assertEquals(expectedBonus, actualBonus,
              "Nova CEWS bonus should be 5% of total friendly Nova CEWS force BV");
    }

    /**
     * Three units with Nova CEWS should get 5% of total force BV as bonus.
     */
    @Test
    void testGetExtraC3BV_ThreeUnitNetwork() {
        Entity pariah1 = loadPariah();
        Entity pariah2 = loadPariah();
        Entity pariah3 = loadPariah();
        game.addEntity(pariah1);
        game.addEntity(pariah2);
        game.addEntity(pariah3);

        int baseBV1 = pariah1.calculateBattleValue(true, true);
        int baseBV2 = pariah2.calculateBattleValue(true, true);
        int baseBV3 = pariah3.calculateBattleValue(true, true);

        // Expected: (baseBV1 + baseBV2 + baseBV3) * 0.05
        int totalForceBV = baseBV1 + baseBV2 + baseBV3;
        int expectedBonus = (int) Math.round(totalForceBV * 0.05);

        int actualBonus = pariah1.getExtraC3BV(baseBV1);

        assertEquals(expectedBonus, actualBonus,
              "Nova CEWS bonus should be 5% of total friendly Nova CEWS force BV");
    }

    /**
     * Bonus should be capped at 35% of unit's base BV when total force BV is high.
     * With 8+ Pariah units, raw 5% bonus would exceed 35% cap.
     */
    @Test
    void testGetExtraC3BV_CappedAt35Percent() {
        Entity pariah1 = loadPariah();
        game.addEntity(pariah1);

        // Add enough units to exceed 35% cap
        // 35% cap = 2388 * 0.35 = 835.8 -> 836
        // To exceed: need totalForceBV * 0.05 > 836
        // totalForceBV > 16720, so need 8+ units (8 * 2388 = 19104)
        for (int i = 0; i < 8; i++) {
            Entity pariah = loadPariah();
            game.addEntity(pariah);
        }

        int baseBV = pariah1.calculateBattleValue(true, true);
        int maxBonus = (int) Math.round(baseBV * 0.35);

        int actualBonus = pariah1.getExtraC3BV(baseBV);

        assertEquals(maxBonus, actualBonus,
              "Nova CEWS bonus should be capped at 35% of base BV (" + maxBonus + ")");
    }

    /**
     * Entity without Nova CEWS should get no C3 bonus from Nova CEWS units.
     */
    @Test
    void testGetExtraC3BV_NoBonus_WithoutNovaCEWS() {
        // Load an entity without Nova CEWS
        Entity exterminator = EntityLoader.loadFromFile("Exterminator EXT-4A.mtf");
        exterminator.setGame(game);
        exterminator.setOwner(game.getPlayer(0));
        exterminator.setId(nextEntityId++);
        game.addEntity(exterminator);

        // Add some Pariah units with Nova CEWS
        Entity pariah1 = loadPariah();
        Entity pariah2 = loadPariah();
        game.addEntity(pariah1);
        game.addEntity(pariah2);

        int baseBV = exterminator.calculateBattleValue(true, true);
        int extraBV = exterminator.getExtraC3BV(baseBV);

        assertEquals(0, extraBV,
              "Entity without Nova CEWS should get no bonus from Nova CEWS units");
    }
}

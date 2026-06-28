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
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for C3 Master (C3M) and Company Commander (C3MM) BV calculation.
 *
 * <p>Per TT Rules: "Two or more units in a battle force equipped with C3 systems
 * can be designated as part of a C3 network. Add 5 percent of the total BV of all units in a C3 network to each of the
 * units linked by each network."</p>
 *
 * <p>This specifically tests the fix for C3M units connected to a C3MM (Company
 * Commander) without any C3S slaves - they should still receive the 5% network bonus as they form a valid 2-unit
 * network.</p>
 */
public class C3MasterBVTest {

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
     * Creates a C3MM (Company Commander) entity with two C3 Master computers.
     */
    private Entity createC3MMEntity() {
        Entity entity = new BipedMek();
        entity.setGame(game);
        entity.setId(nextEntityId++);
        entity.setChassis("Test Mek");
        entity.setModel("C3MM");

        Crew crew = new Crew(CrewType.SINGLE);
        entity.setCrew(crew);
        entity.setOwner(game.getPlayer(0));
        entity.setWeight(75.0);
        entity.setOriginalWalkMP(4);

        // Add TWO C3 Master computers to make it a Company Commander (C3MM)
        try {
            EquipmentType c3m = EquipmentType.get("ISC3MasterComputer");
            entity.addEquipment(c3m, Mek.LOC_HEAD);
            entity.addEquipment(c3m, Mek.LOC_CENTER_TORSO);
        } catch (Exception e) {
            fail("Failed to add C3 Master equipment: " + e.getMessage());
        }

        return entity;
    }

    /**
     * Creates a C3M (Lance Master) entity with one C3 Master computer.
     */
    private Entity createC3MEntity() {
        Entity entity = new BipedMek();
        entity.setGame(game);
        entity.setId(nextEntityId++);
        entity.setChassis("Test Mek");
        entity.setModel("C3M");

        Crew crew = new Crew(CrewType.SINGLE);
        entity.setCrew(crew);
        entity.setOwner(game.getPlayer(0));
        entity.setWeight(50.0);
        entity.setOriginalWalkMP(5);

        // Add ONE C3 Master computer (Lance Master)
        try {
            EquipmentType c3m = EquipmentType.get("ISC3MasterComputer");
            entity.addEquipment(c3m, Mek.LOC_HEAD);
        } catch (Exception e) {
            fail("Failed to add C3 Master equipment: " + e.getMessage());
        }

        return entity;
    }

    /**
     * Creates a C3S (Slave) entity with one C3 Slave computer.
     */
    private Entity createC3SEntity() {
        Entity entity = new BipedMek();
        entity.setGame(game);
        entity.setId(nextEntityId++);
        entity.setChassis("Test Mek");
        entity.setModel("C3S");

        Crew crew = new Crew(CrewType.SINGLE);
        entity.setCrew(crew);
        entity.setOwner(game.getPlayer(0));
        entity.setWeight(50.0);
        entity.setOriginalWalkMP(5);

        // Add C3 Slave computer
        try {
            EquipmentType c3s = EquipmentType.get("ISC3SlaveUnit");
            entity.addEquipment(c3s, Mek.LOC_HEAD);
        } catch (Exception e) {
            fail("Failed to add C3 Slave equipment: " + e.getMessage());
        }

        return entity;
    }

    /**
     * Verify test entities have correct C3 types.
     */
    @Test
    void testC3TypeDetection() {
        Entity c3mm = createC3MMEntity();
        Entity c3m = createC3MEntity();
        Entity c3s = createC3SEntity();
        game.addEntity(c3mm);
        game.addEntity(c3m);
        game.addEntity(c3s);

        assertTrue(c3mm.hasC3MM(), "Entity with two C3M should be C3MM (Company Commander)");
        assertTrue(c3mm.hasC3M(), "C3MM should also report hasC3M()");

        assertTrue(c3m.hasC3M(), "Entity with one C3M should be C3M (Lance Master)");
        assertFalse(c3m.hasC3MM(), "Entity with one C3M should NOT be C3MM");

        assertTrue(c3s.hasC3S(), "Entity with C3S should report hasC3S()");
        assertFalse(c3s.hasC3M(), "C3S entity should NOT report hasC3M()");
    }

    /**
     * Single C3MM with no connections should get no bonus.
     */
    @Test
    void testGetExtraC3BV_NoBonus_SingleC3MM() {
        Entity c3mm = createC3MMEntity();
        game.addEntity(c3mm);

        int baseBV = c3mm.calculateBattleValue(true, true);
        int extraBV = c3mm.getExtraC3BV(baseBV);

        assertEquals(0, extraBV, "Single C3MM with no connections should get no bonus");
    }

    /**
     * Single C3M with no connections should get no bonus.
     */
    @Test
    void testGetExtraC3BV_NoBonus_SingleC3M() {
        Entity c3m = createC3MEntity();
        game.addEntity(c3m);

        int baseBV = c3m.calculateBattleValue(true, true);
        int extraBV = c3m.getExtraC3BV(baseBV);

        assertEquals(0, extraBV, "Single C3M with no connections should get no bonus");
    }

    /**
     * C3MM connected to C3M (no slaves) should form valid 2-unit network. Both units should receive 5% of combined
     * network BV.
     *
     * <p>This is the key test for the bug fix - a C3M connected to a C3MM
     * without any C3S slaves should still receive the network bonus.</p>
     */
    @Test
    void testGetExtraC3BV_C3MMWithC3M_NoSlaves() {
        Entity c3mm = createC3MMEntity();
        Entity c3m = createC3MEntity();
        game.addEntity(c3mm);
        game.addEntity(c3m);

        // Connect C3M to C3MM
        c3m.setC3Master(c3mm.getId(), true);

        // Verify connection
        assertEquals(c3mm, c3m.getC3Master(), "C3M should be connected to C3MM");
        assertTrue(c3m.onSameC3NetworkAs(c3mm), "C3M and C3MM should be on same network");
        assertTrue(c3mm.onSameC3NetworkAs(c3m), "C3MM and C3M should be on same network");

        int baseBVC3MM = c3mm.calculateBattleValue(true, true);
        int baseBVC3M = c3m.calculateBattleValue(true, true);
        int totalNetworkBV = baseBVC3MM + baseBVC3M;
        int expectedBonus = (int) Math.round(totalNetworkBV * 0.05);

        // Both units should receive the 5% network bonus
        int c3mmBonus = c3mm.getExtraC3BV(baseBVC3MM);
        int c3mBonus = c3m.getExtraC3BV(baseBVC3M);

        assertEquals(expectedBonus, c3mmBonus,
              "C3MM should receive 5% of network BV (" + totalNetworkBV + " * 0.05 = " + expectedBonus + ")");
        assertEquals(expectedBonus, c3mBonus,
              "C3M connected to C3MM (no slaves) should receive 5% of network BV");
    }

    /**
     * C3M with C3S slaves (traditional setup) should receive bonus.
     */
    @Test
    void testGetExtraC3BV_C3MWithSlaves() {
        Entity c3m = createC3MEntity();
        Entity c3s1 = createC3SEntity();
        Entity c3s2 = createC3SEntity();
        game.addEntity(c3m);
        game.addEntity(c3s1);
        game.addEntity(c3s2);

        // Connect slaves to master
        c3s1.setC3Master(c3m.getId(), true);
        c3s2.setC3Master(c3m.getId(), true);

        int baseBVC3M = c3m.calculateBattleValue(true, true);
        int baseBVC3S1 = c3s1.calculateBattleValue(true, true);
        int baseBVC3S2 = c3s2.calculateBattleValue(true, true);
        int totalNetworkBV = baseBVC3M + baseBVC3S1 + baseBVC3S2;
        int expectedBonus = (int) Math.round(totalNetworkBV * 0.05);

        int c3mBonus = c3m.getExtraC3BV(baseBVC3M);

        assertEquals(expectedBonus, c3mBonus,
              "C3M with slaves should receive 5% of network BV");
    }

    /**
     * Full company network: C3MM -> C3M -> C3S slaves. All units should receive 5% of total network BV.
     */
    @Test
    void testGetExtraC3BV_FullCompanyNetwork() {
        Entity c3mm = createC3MMEntity();
        Entity c3m = createC3MEntity();
        Entity c3s1 = createC3SEntity();
        Entity c3s2 = createC3SEntity();
        game.addEntity(c3mm);
        game.addEntity(c3m);
        game.addEntity(c3s1);
        game.addEntity(c3s2);

        // Connect C3M to C3MM
        c3m.setC3Master(c3mm.getId(), true);
        // Connect slaves to C3M
        c3s1.setC3Master(c3m.getId(), true);
        c3s2.setC3Master(c3m.getId(), true);

        int baseBVC3MM = c3mm.calculateBattleValue(true, true);
        int baseBVC3M = c3m.calculateBattleValue(true, true);
        int baseBVC3S1 = c3s1.calculateBattleValue(true, true);
        int baseBVC3S2 = c3s2.calculateBattleValue(true, true);
        int totalNetworkBV = baseBVC3MM + baseBVC3M + baseBVC3S1 + baseBVC3S2;
        int expectedBonus = (int) Math.round(totalNetworkBV * 0.05);

        int c3mmBonus = c3mm.getExtraC3BV(baseBVC3MM);
        int c3mBonus = c3m.getExtraC3BV(baseBVC3M);
        int c3s1Bonus = c3s1.getExtraC3BV(baseBVC3S1);
        int c3s2Bonus = c3s2.getExtraC3BV(baseBVC3S2);

        assertEquals(expectedBonus, c3mmBonus, "C3MM should receive 5% of full network BV");
        assertEquals(expectedBonus, c3mBonus, "C3M should receive 5% of full network BV");
        assertEquals(expectedBonus, c3s1Bonus, "C3S should receive 5% of full network BV");
        assertEquals(expectedBonus, c3s2Bonus, "C3S should receive 5% of full network BV");
    }
}

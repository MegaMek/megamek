/*
 * Copyright (c) 2000-2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.common.battlevalue.BVCalculator;
import megamek.common.equipment.WeaponMounted;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 11/3/13 8:48 AM
 */
class EntityTest {

    private Entity setupGunEmplacement() {
        Entity testEntity = mock(GunEmplacement.class);
        when(testEntity.getBvCalculator()).thenReturn(BVCalculator.getBVCalculator(testEntity));
        when(testEntity.calculateBattleValue()).thenCallRealMethod();
        when(testEntity.calculateBattleValue(anyBoolean(), anyBoolean())).thenCallRealMethod();
        when(testEntity.doBattleValueCalculation(anyBoolean(), anyBoolean(),
                any(CalculationReport.class))).thenCallRealMethod();
        when(testEntity.getTotalArmor()).thenReturn(100);
        when(testEntity.getBARRating(anyInt())).thenCallRealMethod();
        List<Mounted<?>> equipment = new ArrayList<>(2);
        List<WeaponMounted> weapons = new ArrayList<>(2);
        WeaponType ppcType = mock(WeaponType.class);
        when(ppcType.getBV(any(Entity.class))).thenReturn(50.0);
        WeaponMounted ppc = mock(WeaponMounted.class);
        when(ppc.getType()).thenReturn(ppcType);
        when(ppc.isDestroyed()).thenReturn(false);
        equipment.add(ppc);
        equipment.add(ppc);
        weapons.add(ppc);
        weapons.add(ppc);
        when(testEntity.getEquipment()).thenReturn(equipment);
        when(testEntity.getWeaponList()).thenReturn(weapons);
        when(testEntity.getAmmo()).thenReturn(new ArrayList<>(0));
        return testEntity;
    }

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @Test
    void testCalculateBattleValue() {
        // Test a gun emplacement.
        Entity testEntity = setupGunEmplacement();
        int expected = 169;
        int actual = testEntity.calculateBattleValue(true, true);
        assertEquals(expected, actual);
        when(testEntity.getTotalArmor()).thenReturn(0); // Gun Emplacement with no armor.
        expected = 44;
        actual = testEntity.calculateBattleValue(true, true);
        assertEquals(expected, actual);
    }

    @Test
    void testCalculateWeight() {
        File f;
        MekFileParser mfp;
        Entity e;
        int expectedWeight, computedWeight;

        // Test 1/1
        try {
            f = new File("testresources/megamek/common/units/Exterminator EXT-4A.mtf");
            mfp = new MekFileParser(f);
            e = mfp.getEntity();
            expectedWeight = 65;
            computedWeight = (int) e.getWeight();
            assertEquals(expectedWeight, computedWeight);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    void testFormatHeat() {
        File f;
        MekFileParser mfp;
        Entity e;
        String expectedHeat, computedHeat;

        try {
            f = new File("testresources/megamek/common/units/Sagittaire SGT-14D.mtf");
            mfp = new MekFileParser(f);
            e = mfp.getEntity();
            expectedHeat = "28, 42 with RHS";
            computedHeat = e.formatHeat();
            assertEquals(expectedHeat, computedHeat);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Verify that if a unit's name appears in the list of canon unit names, it is
     * canon
     */
    @Test
    void testCanon() {
        File f;
        MekFileParser mfp;
        Entity e;
        Vector<String> unitNames = new Vector<>();
        unitNames.add("Exterminator EXT-4A");
        MekFileParser.setCanonUnitNames(unitNames);

        // Test 1/1
        try {
            f = new File("testresources/megamek/common/units/Exterminator EXT-4A.mtf");
            mfp = new MekFileParser(f);
            e = mfp.getEntity();
            assertTrue(e.isCanon());
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Verify that if a unit's name does _not_ appear in the list of canon unit
     * names, it is not canon
     */
    @Test
    void testForceCanonicityFailure() {
        File f;
        MekFileParser mfp;
        Entity e;
        Vector<String> unitNames = new Vector<>();
        unitNames.add("Beheadanator BHD-999.666Z");
        MekFileParser.setCanonUnitNames(unitNames);

        try {
            f = new File("testresources/megamek/common/units/Exterminator EXT-4A.mtf");
            mfp = new MekFileParser(f);
            e = mfp.getEntity();
            assertFalse(e.isCanon());
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Verify that if a unit's name does appear in the _file_ listing canon unit
     * names, it is canon
     */
    @Test
    void testCanonUnitInCanonUnitListFile() {
        File f;
        MekFileParser mfp;
        Entity e;
        File oulDir = new File("testresources/megamek/common/units/");
        MekFileParser.initCanonUnitNames(oulDir, "mockOfficialUnitList.txt");

        try {
            // MTF file check
            f = new File("testresources/megamek/common/units/Exterminator EXT-4A.mtf");
            mfp = new MekFileParser(f);
            e = mfp.getEntity();
            assertTrue(e.isCanon());
            // BLK file check
            f = new File("testresources/megamek/common/units/Kanga Medium Hovertank.blk");
            mfp = new MekFileParser(f);
            e = mfp.getEntity();
            assertTrue(e.isCanon());
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Verify new Tank method .isImmobilizedForJump() returns correct values in
     * various states. Note: vehicles cannot lose individual Jump Jets via crits,
     * so this is not tested.
     */
    @Test
    void testIsImmobilizedForJump() {
        File f;
        MekFileParser mfp;
        Entity e;

        // Test 1/1
        try {
            f = new File("testresources/megamek/common/units/Kanga Medium Hovertank.blk");
            mfp = new MekFileParser(f);
            e = mfp.getEntity();
            Tank t = (Tank) e;
            Crew c = t.getCrew();

            // 1 Crew condition
            // 1.a Killed crew should prevent jumping; live crew should allow jumping
            c.setDead(true);
            assertTrue(t.isImmobileForJump());
            c.resetGameState();
            assertFalse(t.isImmobileForJump());

            // 1.b Unconscious crew should prevent jumping; conscious crew should allow
            // jumping
            c.setUnconscious(true);
            assertTrue(t.isImmobileForJump());
            c.resetGameState();
            assertFalse(t.isImmobileForJump());

            // 1.c Stunned crew should _not_ prevent jumping
            t.setStunnedTurns(1);
            assertFalse(t.isImmobileForJump());
            t.setStunnedTurns(0);

            // 2. Engine condition
            // 2.a Engine hit should prevent jumping; fixing engine should enable jumping
            t.engineHit();
            assertTrue(t.isImmobileForJump());
            t.engineFix();
            assertFalse(t.isImmobileForJump());

            // 2.b Shutdown should prevent jumping; restarting should enable jumping
            t.setShutDown(true);
            assertTrue(t.isImmobileForJump());
            t.setShutDown(false);
            assertFalse(t.isImmobileForJump());

            // 3. Immobilization due to massive damage motive hit / reducing MP to 0 should
            // _not_ prevent jumping
            t.setMotiveDamage(t.getOriginalWalkMP());
            assertFalse(t.isImmobileForJump());
            t.setMotiveDamage(0);

        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }
}

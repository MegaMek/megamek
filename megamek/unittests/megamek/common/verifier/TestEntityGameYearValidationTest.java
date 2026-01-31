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
package megamek.common.verifier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import megamek.common.TechConstants;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.Mek;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests for TestEntity game year validation functionality.
 * Verifies that hasIncorrectIntroYear() correctly uses the game year setting
 * when validating equipment intro dates, and that retrofittable equipment
 * (DNI, EI) is skipped during validation.
 */
class TestEntityGameYearValidationTest {

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    /**
     * Creates a basic Mek for testing with the specified intro year.
     */
    private Mek createTestMek(int introYear) {
        Mek mek = new BipedMek();
        mek.setChassis("Test");
        mek.setModel("Mek");
        mek.setYear(introYear);
        mek.setWeight(20.0);
        mek.setEngine(new Engine(100, Engine.NORMAL_ENGINE, 0));
        return mek;
    }

    /**
     * Creates a TestMek verifier for the given Mek.
     */
    private TestMek createTestEntity(Mek mek) {
        EntityVerifier verifier = EntityVerifier.getInstance(
                new java.io.File("testresources/data/mekfiles/UnitVerifierOptions.xml"));
        return new TestMek(mek, verifier.mekOption, null);
    }

    // ========== Game Year Setter/Getter Tests ==========

    @Test
    void testGameYearDefaultsToNegativeOne() {
        Mek mek = createTestMek(3025);
        TestMek testEntity = createTestEntity(mek);

        assertEquals(-1, testEntity.getGameYear(), "Game year should default to -1");
    }

    @Test
    void testSetGameYear() {
        Mek mek = createTestMek(3025);
        TestMek testEntity = createTestEntity(mek);

        testEntity.setGameYear(3050);
        assertEquals(3050, testEntity.getGameYear(), "Game year should be set to 3050");
    }

    // ========== DNI Retrofittable Equipment Tests ==========

    @Test
    void testDNISkippedAsRetrofittableEquipment() throws Exception {
        // Create a Mek with intro year well before DNI (3052)
        // DNI should be skipped during validation because it's retrofittable
        Mek mek = createTestMek(2500);
        MiscType dniMod = (MiscType) EquipmentType.get("DNICockpitModification");
        mek.addEquipment(dniMod, Entity.LOC_NONE);

        TestMek testEntity = createTestEntity(mek);
        // Don't set game year - use entity year (2500)
        // DNI intro is 3052, but it should be skipped as retrofittable

        StringBuffer buff = new StringBuffer();
        boolean hasIncorrectIntro = testEntity.hasIncorrectIntroYear(buff);

        // DNI is retrofittable equipment, so it should be skipped in validation
        assertFalse(hasIncorrectIntro,
                "DNI should not cause validation failure (retrofittable equipment): " + buff);
    }

    @Test
    void testDNISkippedEvenWithEarlyGameYear() throws Exception {
        // Create a Mek with intro year before DNI (3052)
        Mek mek = createTestMek(2500);
        MiscType dniMod = (MiscType) EquipmentType.get("DNICockpitModification");
        mek.addEquipment(dniMod, Entity.LOC_NONE);

        TestMek testEntity = createTestEntity(mek);
        // Set game year BEFORE DNI intro - but DNI should still pass because it's retrofittable
        testEntity.setGameYear(3000);

        StringBuffer buff = new StringBuffer();
        boolean hasIncorrectIntro = testEntity.hasIncorrectIntroYear(buff);

        // DNI is retrofittable equipment, so it should be skipped in validation
        assertFalse(hasIncorrectIntro,
                "DNI should not cause validation failure even with early game year (retrofittable): " + buff);
    }

    // ========== EI Interface Retrofittable Equipment Tests ==========

    @Test
    void testEISkippedAsRetrofittableEquipment() throws Exception {
        // Create a Clan Mek with intro year well before EI (3040)
        Mek mek = createTestMek(2800);
        mek.setTechLevel(TechConstants.T_CLAN_TW); // EI is Clan only
        MiscType eiInterface = (MiscType) EquipmentType.get("EIInterface");
        mek.addEquipment(eiInterface, Entity.LOC_NONE);

        TestMek testEntity = createTestEntity(mek);
        // Don't set game year - use entity year (2800)
        // EI intro is 3040, but it should be skipped as retrofittable

        StringBuffer buff = new StringBuffer();
        boolean hasIncorrectIntro = testEntity.hasIncorrectIntroYear(buff);

        // EI is retrofittable equipment, so it should be skipped in validation
        assertFalse(hasIncorrectIntro,
                "EI should not cause validation failure (retrofittable equipment): " + buff);
    }

    @Test
    void testEISkippedEvenWithEarlyGameYear() throws Exception {
        // Create a Clan Mek with intro year before EI (3040)
        Mek mek = createTestMek(2800);
        mek.setTechLevel(TechConstants.T_CLAN_TW); // EI is Clan only
        MiscType eiInterface = (MiscType) EquipmentType.get("EIInterface");
        mek.addEquipment(eiInterface, Entity.LOC_NONE);

        TestMek testEntity = createTestEntity(mek);
        // Set game year BEFORE EI intro - but EI should still pass because it's retrofittable
        testEntity.setGameYear(3000);

        StringBuffer buff = new StringBuffer();
        boolean hasIncorrectIntro = testEntity.hasIncorrectIntroYear(buff);

        // EI is retrofittable equipment, so it should be skipped in validation
        assertFalse(hasIncorrectIntro,
                "EI should not cause validation failure even with early game year (retrofittable): " + buff);
    }

    // ========== Game Year Field Tests ==========

    @Test
    void testGameYearFieldIsUsedWhenSet() {
        // Verify that when setGameYear is called, the value is stored and retrievable
        Mek mek = createTestMek(3025);
        TestMek testEntity = createTestEntity(mek);

        // Initially should be -1
        assertEquals(-1, testEntity.getGameYear());

        // Set to a specific year
        testEntity.setGameYear(3145);
        assertEquals(3145, testEntity.getGameYear());

        // Can be reset
        testEntity.setGameYear(3050);
        assertEquals(3050, testEntity.getGameYear());

        // Can be reset to -1 (use entity year)
        testEntity.setGameYear(-1);
        assertEquals(-1, testEntity.getGameYear());
    }

    @Test
    void testMultipleCockpitModsAllSkipped() throws Exception {
        // Create a Mek with both DNI and EI (theoretically)
        // Both should be skipped as retrofittable
        Mek mek = createTestMek(2500);
        mek.setTechLevel(TechConstants.T_CLAN_TW);
        MiscType dniMod = (MiscType) EquipmentType.get("DNICockpitModification");
        MiscType eiInterface = (MiscType) EquipmentType.get("EIInterface");
        mek.addEquipment(dniMod, Entity.LOC_NONE);
        mek.addEquipment(eiInterface, Entity.LOC_NONE);

        TestMek testEntity = createTestEntity(mek);
        testEntity.setGameYear(3000); // Before both DNI (3052) and EI (3040) intros

        StringBuffer buff = new StringBuffer();
        boolean hasIncorrectIntro = testEntity.hasIncorrectIntroYear(buff);

        // Both should be skipped as retrofittable
        assertFalse(hasIncorrectIntro,
                "Both DNI and EI should be skipped as retrofittable: " + buff);
    }
}

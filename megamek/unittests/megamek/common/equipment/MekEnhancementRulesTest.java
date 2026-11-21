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
package megamek.common.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import megamek.common.CriticalSlot;
import megamek.common.exceptions.LocationFullException;
import megamek.common.units.BipedMek;
import megamek.common.units.Mek;
import megamek.common.units.QuadMek;
import megamek.common.units.TripodMek;
import megamek.common.verifier.EntityVerifier;
import megamek.common.verifier.TestMek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Rules-text checks for Mek leg equipment, Coolant Pods and RISC Laser Pulse Modules. */
class MekEnhancementRulesTest {

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @Test
    void legEquipmentTakesTwoSlotsPerLegAndOnePerSuperheavyLeg() {
        // TO:AUE pp. 104-105 and IO:AE p. 157
        EquipmentType booster = EquipmentType.get(EquipmentTypeLookup.MECHANICAL_JUMP_BOOSTER);
        Mek biped = new BipedMek();
        biped.setWeight(50);
        Mek quad = new QuadMek();
        quad.setWeight(50);
        Mek tripod = new TripodMek();
        tripod.setWeight(50);
        Mek superheavy = new BipedMek();
        superheavy.setWeight(150);

        assertEquals(4, booster.getNumCriticalSlots(biped, 1));
        assertEquals(8, booster.getNumCriticalSlots(quad, 1));
        assertEquals(6, booster.getNumCriticalSlots(tripod, 1));
        assertEquals(2, EquipmentType.get("Talons").getNumCriticalSlots(superheavy, 1));
    }

    @Test
    void verifierRequiresMechanicalJumpBoosterSlotsInEachLeg() throws LocationFullException {
        int slots = 2;
        Mek mek = new BipedMek();
        mek.setWeight(50);
        mek.setStructureType(EquipmentType.T_STRUCTURE_STANDARD);
        mek.setEngine(new Engine(250, Engine.NORMAL_ENGINE, 0));
        EquipmentType booster = EquipmentType.get(EquipmentTypeLookup.MECHANICAL_JUMP_BOOSTER);
        assertFalse(TestMek.isValidMekLocation(mek, booster, Mek.LOC_RIGHT_TORSO, null));
        assertTrue(TestMek.isValidMekLocation(mek, booster, Mek.LOC_RIGHT_LEG, null));

        Mounted<?> mount = mek.addEquipment(booster, Mek.LOC_NONE);
        EntityVerifier verifier = EntityVerifier.getInstance(
              new File("testresources/data/mekfiles/UnitVerifierOptions.xml"));
        TestMek testMek = new TestMek(mek, verifier.mekOption, null);
        String message = "2 critical slots in each leg";
        StringBuffer report = new StringBuffer();
        for (int i = 0; i < slots; i++) {
            mek.addCritical(Mek.LOC_RIGHT_LEG, new CriticalSlot(mount));
        }
        testMek.hasIllegalEquipmentCombinations(report);
        assertTrue(report.toString().contains(message));

        for (int i = 0; i < slots; i++) {
            mek.addCritical(Mek.LOC_LEFT_LEG, new CriticalSlot(mount));
        }
        report.setLength(0);
        testMek.hasIllegalEquipmentCombinations(report);
        assertFalse(report.toString().contains(message));
    }

    /** IO:AE p. 156: superheavy Meks "may not mount jump jets, improved jump jets, jump boosters, or partial wings" */
    @Test
    void verifierRejectsMechanicalJumpBoostersOnSuperheavyMeks() throws LocationFullException {
        String message = "Superheavy may not mount "
              + EquipmentType.get(EquipmentTypeLookup.MECHANICAL_JUMP_BOOSTER).getName();

        assertTrue(mechanicalJumpBoosterReport(105).contains(message));
        assertFalse(mechanicalJumpBoosterReport(100).contains(message));
    }

    private static String mechanicalJumpBoosterReport(double weight) throws LocationFullException {
        Mek mek = new BipedMek();
        mek.setWeight(weight);
        mek.setStructureType(EquipmentType.T_STRUCTURE_STANDARD);
        mek.setEngine(new Engine(300, Engine.NORMAL_ENGINE, 0));
        mek.addEquipment(EquipmentType.get(EquipmentTypeLookup.MECHANICAL_JUMP_BOOSTER), Mek.LOC_NONE);
        EntityVerifier verifier = EntityVerifier.getInstance(
              new File("testresources/data/mekfiles/UnitVerifierOptions.xml"));
        StringBuffer report = new StringBuffer();
        new TestMek(mek, verifier.mekOption, null).hasIllegalEquipmentCombinations(report);
        return report.toString();
    }

    @Test
    void coolantPodCountsEveryActiveHeatSinkType() throws LocationFullException {
        // TO:AUE p. 116: +1 per active heat sink, regardless of type
        Mek mek = new BipedMek();
        mek.setWeight(50);
        Mounted<?> single = mek.addEquipment(EquipmentType.get("Heat Sink"), Mek.LOC_LEFT_TORSO);
        mek.addEquipment(EquipmentType.get("Heat Sink"), Mek.LOC_LEFT_TORSO);
        mek.addEquipment(EquipmentType.get(EquipmentTypeLookup.IS_DOUBLE_HS_PROTOTYPE), Mek.LOC_RIGHT_TORSO);

        Mounted<?> pair = mek.addEquipment(EquipmentType.get(EquipmentTypeLookup.COMPACT_HS_2), Mek.LOC_LEFT_ARM);
        mek.addEquipment(EquipmentType.get(EquipmentTypeLookup.IS_DOUBLE_HS_FREEZER), Mek.LOC_RIGHT_ARM);

        assertEquals(3, mek.getActiveSinks());
        assertEquals(6, mek.getActiveSinksOfAnyType());
        pair.setModeImmediately(Mounted.MODE_OFF);
        assertEquals(4, mek.getActiveSinksOfAnyType());

        single.setDestroyed(true);
        assertEquals(3, mek.getActiveSinksOfAnyType());
    }

    @Test
    void pulseModuleCostsItsListedPrice() {
        // IO:AE equipment cost table: 200,000 C-bills
        assertEquals(200000, EquipmentType.get("ISRISCLaserPulseModule").getRawCost());
    }

    @Test
    void pulseModuleFitsOnlyInnerSphereStandardAndErLasers() {
        // IO:AE p. 87; primitive prototype weapons can't take modern targeting enhancements (IO:AE p. 112)
        for (String laser : new String[] { "Medium Laser", "ISERLargeLaser", "ISERLargeLaserPrototype" }) {
            assertTrue(EquipmentType.get(laser).hasFlag(WeaponType.F_RISC_LASER_PULSE_COMPATIBLE), laser);
        }
        for (String laser : new String[] { "ISMediumPulseLaser", "CLERMediumLaser", "ISBombastLaser",
                                           "ISRISCHyperLaser", "Primitive Prototype Small Laser" }) {
            assertFalse(EquipmentType.get(laser).hasFlag(WeaponType.F_RISC_LASER_PULSE_COMPATIBLE), laser);
        }
    }
}

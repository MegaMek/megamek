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

package megamek.common.battleValue;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import megamek.client.ui.clientGUI.calculationReport.TextCalculationReport;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.units.BipedMek;
import megamek.common.units.Mek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Regression tests for reduced explosive-equipment BV penalties on Meks. */
class MekExplosiveEquipmentBVTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    static Stream<Arguments> reducedPenaltyWeapons() {
        return Stream.of(
              Arguments.of("ISGaussRifle", 7),
              Arguments.of("ISRISCHyperLaser", 6),
              Arguments.of("TSEMP Cannon", 5),
              Arguments.of("Mek Taser", 3),
              Arguments.of("CLImprovedSmallHeavyLaser", 1));
    }

    static Stream<String> caseIIProtectedWeapons() {
        return Stream.of("ISGaussRifle", "ISRISCHyperLaser", "TSEMP Cannon", "Mek Taser",
              "CLImprovedSmallHeavyLaser");
    }

        @ParameterizedTest(name = "{0} has its reduced explosive penalty")
    @MethodSource("reducedPenaltyWeapons")
        void reducedPenaltyWeaponsDeductOneBVPerCriticalSlot(String weaponName, int expectedPenalty) throws Exception {
        Mounted<?> weapon = addWeapon(weaponName);

        String report = explosiveEquipmentReport(weapon);

          assertTrue(report.matches("(?s).*Explosive Equipment:.*"
                  + java.util.regex.Pattern.quote("- " + weapon.getType().getShortName())
                  + ".*?- " + expectedPenalty + "\\s+.*"),
              () -> weaponName + " should receive the reduced explosive-equipment penalty.\n" + report);
    }

        @ParameterizedTest(name = "{0} remains protected by CASE II")
    @MethodSource("caseIIProtectedWeapons")
    void caseIIRemovesExplosiveEquipmentPenalty(String weaponName) throws Exception {
          Mounted<?> weapon = addWeapon(weaponName);
        Mek mek = (Mek) weapon.getEntity();
          mek.addEquipment(EquipmentType.get("ISCASEII"), Mek.LOC_RIGHT_ARM);

        String report = explosiveEquipmentReport(weapon);

          assertTrue(!report.contains("Explosive Equipment:"),
              () -> weaponName + " in a CASE II-protected location must not receive an explosive-equipment penalty.\n" + report);
    }

    private Mounted<?> addWeapon(String weaponName) throws Exception {
        Mek mek = createMek();
        EquipmentType equipmentType = EquipmentType.get(weaponName);
        return mek.addEquipment(equipmentType, Mek.LOC_RIGHT_ARM);
    }

    private Mek createMek() {
        Mek mek = new BipedMek();
        mek.setChassis("Test");
        mek.setModel("Explosive Equipment");
        mek.setWeight(100.0);
        mek.setOriginalWalkMP(3);
        mek.setEngine(new Engine(300, Engine.NORMAL_ENGINE, 0));
        mek.addCockpit();
        mek.addGyro();
        mek.addEngineCrits();
        mek.autoSetInternal();
        for (int location = 0; location < mek.locations(); location++) {
            mek.initializeArmor(20, location);
            if (mek.hasRearArmor(location)) {
                mek.initializeRearArmor(10, location);
            }
        }
        return mek;
    }

    private String explosiveEquipmentReport(Mounted<?> weapon) {
        TextCalculationReport report = new TextCalculationReport();
        BVCalculator.getBVCalculator(weapon.getEntity()).calculateBaseBV(report);
        return report.toString();
    }
}

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
package megamek.common.verifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.TechConstants;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.EquipmentType;
import megamek.common.units.EntityWeightClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TestBattleArmorWeightTest {
    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @Test
    void weighsDetachablePackWeaponWithoutASecondSuitLocation() throws Exception {
        var armor = new BattleArmor();
        armor.setSquadSize(4);
        armor.refreshLocations();
        var weapon = armor.addEquipment(EquipmentType.get("ISBATaser"), 4);
        weapon.setDWPMounted(true);
        assertEquals(BattleArmor.MOUNT_LOC_NONE, weapon.getBaMountLoc());
        var verifier = new TestBattleArmor(armor, new TestXMLOption(), "");

        assertEquals(0.225, verifier.getWeightWeapon(4), 0.000001);
        assertEquals(0, verifier.getWeightWeapon(1), 0.000001);
        weapon.setDWPMounted(false);
        assertEquals(0, verifier.getWeightWeapon(4), 0.000001);
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 5, 6})
    void includesTheFinalTrooperInTotalAndIndividualWeightChecks(int troopers) throws Exception {
        var armor = new BattleArmor();
        armor.setTechLevel(TechConstants.T_IS_ADVANCED);
        armor.setWeightClass(EntityWeightClass.WEIGHT_MEDIUM);
        armor.setSquadSize(troopers);
        armor.refreshLocations();
        armor.setArmorType(EquipmentType.T_ARMOR_BA_STANDARD);
        for (int trooper = 1; trooper <= troopers; trooper++) {
            armor.initializeArmor(0, trooper);
        }
        armor.autoSetInternal();
        // Only the final suit carries the extra one-ton weapon. The formation still has spare mass.
        var weapon = armor.addEquipment(EquipmentType.get("CLBAHeavyMediumLaser"), troopers);
        weapon.setBaMountLoc(BattleArmor.MOUNT_LOC_BODY);
        var verifier = new TestBattleArmor(armor, new TestXMLOption(), "");
        double ordinarySuit = verifier.calculateWeight(1);
        double finalSuit = verifier.calculateWeight(troopers);

        assertEquals(1.0, finalSuit - ordinarySuit, 0.000001);
        assertEquals((troopers - 1) * ordinarySuit + finalSuit, verifier.calculateWeightExact(), 0.000001);
        assertTrue(verifier.calculateWeightExact() < verifier.getWeight());
        var messages = new StringBuffer();
        assertFalse(verifier.correctWeight(messages, true, true));
        assertTrue(messages.toString().contains("Trooper " + troopers + " Weight:"));
    }
}

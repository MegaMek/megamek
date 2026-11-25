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
 */
package megamek.common.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import megamek.common.equipment.EquipmentType;
import megamek.common.rules.core.CoreRulesArmor;
import megamek.common.rules.totalwarfare.TWRulesArmor;
import megamek.common.units.BipedMek;
import megamek.common.units.Mek;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RulesArmor rules variants")
class RulesArmorTest {

    @Test
    @DisplayName("core and total warfare variants construct and inherit from the base rules type")
    void coreAndTotalWarfareVariantsConstructAndInherit() {
        CoreRulesArmor core = new CoreRulesArmor();
        TWRulesArmor totalWarfare = new TWRulesArmor();

        assertNotNull(core, "Core rules implementation should be constructible.");
        assertNotNull(totalWarfare, "Total warfare rules implementation should be constructible.");
        assertInstanceOf(RulesArmor.class, core, "Core rules should extend the base rules type.");
        assertInstanceOf(RulesArmor.class, totalWarfare, "Total warfare rules should extend the base rules type.");
    }

    @Test
    @DisplayName("laser-reflective armor halves heat; heat-dissipating armor meets Extreme Heat only under TW")
    void armorAgainstHeat() {
        Mek mek = new BipedMek();
        mek.setArmorType(EquipmentType.T_ARMOR_HEAT_DISSIPATING);
        for (int location = 0; location < mek.locations(); location++) {
            mek.initializeArmor(1, location);
        }
        TWRulesArmor totalWarfare = new TWRulesArmor();

        // BMM p.114: laser-reflective armor halves the heat of the hit that strikes it, to a minimum of 1
        assertEquals(2, totalWarfare.reduceHeatDamageByArmor(EquipmentType.T_ARMOR_REFLECTIVE, 5));
        assertEquals(1, totalWarfare.reduceHeatDamageByArmor(EquipmentType.T_ARMOR_REFLECTIVE, 1));
        // Core p. 202 keeps Extreme Heat; IO:AE p. 81 halves every external source
        assertEquals(10, new CoreRulesArmor().reduceExtremeHeat(mek, 10));
        assertEquals(5, totalWarfare.reduceExtremeHeat(mek, 10));
    }
}

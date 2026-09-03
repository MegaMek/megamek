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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.equipment.WeaponType;
import megamek.common.rules.core.CoreRulesUnderwater;
import megamek.common.rules.totalwarfare.TWRulesUnderwater;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("RulesUnderwater rules variants")
class RulesUnderwaterTest {

    @Test
    @DisplayName("core and total warfare underwater rules apply different LOS and range behavior")
    void coreAndTotalWarfareUnderwaterRulesApplyDistinctRules() {
        CoreRulesUnderwater core = new CoreRulesUnderwater();
        TWRulesUnderwater totalWarfare = new TWRulesUnderwater();
        WeaponType energyWeapon = Mockito.mock(WeaponType.class);
        WeaponType solidWeapon = Mockito.mock(WeaponType.class);
        Mockito.when(energyWeapon.hasFlag(WeaponType.F_ENERGY)).thenReturn(true);
        Mockito.when(energyWeapon.getShortRange()).thenReturn(4);
        Mockito.when(energyWeapon.getMediumRange()).thenReturn(8);
        Mockito.when(energyWeapon.getLongRange()).thenReturn(12);
        Mockito.when(solidWeapon.hasFlag(WeaponType.F_ENERGY)).thenReturn(false);
        Mockito.when(solidWeapon.getWShortRange()).thenReturn(3);
        Mockito.when(solidWeapon.getWMediumRange()).thenReturn(6);
        Mockito.when(solidWeapon.getWLongRange()).thenReturn(9);
        Mockito.when(solidWeapon.getWExtremeRange()).thenReturn(12);

        assertNotNull(core, "Core rules implementation should be constructible.");
        assertNotNull(totalWarfare, "Total warfare rules implementation should be constructible.");
        assertInstanceOf(RulesUnderwater.class, core, "Core rules should extend the base rules type.");
        assertInstanceOf(RulesUnderwater.class, totalWarfare, "Total warfare rules should extend the base rules type.");

        assertEquals(4, core.getBreachTarget(), "Core underwater breaches are easier to trigger.");
        assertEquals(10, totalWarfare.getBreachTarget(), "Total Warfare underwater breaches are harder to trigger.");
        assertFalse(core.waterBlocksLOS(), "Core rules do not block LOS underwater.");
        assertTrue(totalWarfare.waterBlocksLOS(), "Total Warfare underwater rules block LOS across the waterline.");
        assertEquals(0, core.getShortRange(energyWeapon), "Core energy weapons lose short range underwater.");
        assertEquals(3, core.getShortRange(solidWeapon), "Core ballistic weapons keep their normal short range.");
        assertEquals(4, core.getMediumRange(energyWeapon), "Core energy weapons map medium range to short range underwater.");
        assertEquals(6, core.getMediumRange(solidWeapon), "Core solid weapons keep their normal medium range.");
        assertEquals(8, core.getLongRange(energyWeapon), "Core energy weapons map long range to medium range underwater.");
        assertEquals(9, core.getLongRange(solidWeapon), "Core solid weapons keep their normal long range.");
        assertEquals(12, core.getExtremeRange(energyWeapon), "Core energy weapons map extreme range to long range underwater.");
        assertEquals(12, core.getExtremeRange(solidWeapon), "Core solid weapons keep their normal extreme range.");

        assertEquals(3, totalWarfare.getShortRange(solidWeapon), "Total Warfare keeps the base short range underwater.");
        assertEquals(6, totalWarfare.getMediumRange(solidWeapon), "Total Warfare keeps the base medium range underwater.");
        assertEquals(9, totalWarfare.getLongRange(solidWeapon), "Total Warfare keeps the base long range underwater.");
        assertEquals(12, totalWarfare.getExtremeRange(solidWeapon), "Total Warfare keeps the base extreme range underwater.");
    }
}

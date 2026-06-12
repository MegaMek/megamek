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
package megamek.common.battleArmor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests Battle Armor eligibility to carry Disposable Weapons (TO:AuE p.116, Corrected Sixth Printing): only suits with
 * an anti-personnel weapon mount or two armored gloves may carry them. Armored gloves themselves carry the
 * {@code F_AP_MOUNT} flag, so the "AP mount" path must exclude them.
 */
class BattleArmorDisposableWeaponTest {

    @BeforeAll
    static void initialize() {
        EquipmentType.initializeTypes();
    }

    private static MiscMounted miscWithFlags(boolean apMount, boolean armoredGlove) {
        MiscMounted mount = mock(MiscMounted.class);
        MiscType type = mock(MiscType.class);
        when(mount.getType()).thenReturn(type);
        when(type.hasFlag(MiscType.F_AP_MOUNT)).thenReturn(apMount);
        when(type.hasFlag(MiscType.F_ARMORED_GLOVE)).thenReturn(armoredGlove);
        return mount;
    }

    private static BattleArmor battleArmorWith(List<MiscMounted> misc, int armoredGloveCount) {
        BattleArmor battleArmor = mock(BattleArmor.class);
        when(battleArmor.canCarryDisposableWeapons()).thenCallRealMethod();
        doReturn(misc).when(battleArmor).getMisc();
        doReturn(armoredGloveCount).when(battleArmor).countWorkingMisc(MiscType.F_ARMORED_GLOVE);
        return battleArmor;
    }

    @Test
    @DisplayName("a dedicated anti-personnel weapon mount makes the suit eligible")
    void apMountIsEligible() {
        BattleArmor battleArmor = battleArmorWith(List.of(miscWithFlags(true, false)), 0);
        assertTrue(battleArmor.canCarryDisposableWeapons());
    }

    @Test
    @DisplayName("two armored gloves make the suit eligible")
    void twoArmoredGlovesAreEligible() {
        List<MiscMounted> gloves = List.of(miscWithFlags(true, true), miscWithFlags(true, true));
        BattleArmor battleArmor = battleArmorWith(gloves, 2);
        assertTrue(battleArmor.canCarryDisposableWeapons());
    }

    @Test
    @DisplayName("a single armored glove is NOT enough (and does not count as an AP mount)")
    void singleArmoredGloveIsNotEligible() {
        BattleArmor battleArmor = battleArmorWith(List.of(miscWithFlags(true, true)), 1);
        assertFalse(battleArmor.canCarryDisposableWeapons());
    }

    @Test
    @DisplayName("a suit with neither an AP mount nor armored gloves is not eligible")
    void noMountIsNotEligible() {
        BattleArmor battleArmor = battleArmorWith(List.of(), 0);
        assertFalse(battleArmor.canCarryDisposableWeapons());
    }
}

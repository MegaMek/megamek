/*
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.EnumSet;

import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 9/21/13 8:13 AM
 */
class AmmoTypeTest {
    static WeaponType mockAC5 = mock(WeaponType.class);
    static AmmoType mockAC5AmmoType = mock(AmmoType.class);
    static AmmoMounted mockAmmoAC5 = mock(AmmoMounted.class);
    static AmmoMounted mockAmmoAC5Empty = mock(AmmoMounted.class);
    static AmmoType mockAC10AmmoType = mock(AmmoType.class);
    static AmmoMounted mockAmmoAC10 = mock(AmmoMounted.class);

    static WeaponType mockPPC = mock(WeaponType.class);

    static WeaponType mockSRM4 = mock(WeaponType.class);
    static AmmoType mockSRM4AmmoType = mock(AmmoType.class);
    static AmmoMounted mockAmmoSrm4 = mock(AmmoMounted.class);
    static AmmoType mockSRM6AmmoType = mock(AmmoType.class);
    static AmmoMounted mockAmmoSrm6 = mock(AmmoMounted.class);
    static AmmoType mockInferno4AmmoType = mock(AmmoType.class);
    static AmmoMounted mockAmmoInferno4 = mock(AmmoMounted.class);

    static MiscType notAmmoType = mock(MiscType.class);
    static MiscMounted mockNotAmmo = mock(MiscMounted.class);

    @BeforeAll
    static void beforeAll() {
        when(mockAC5.getAmmoType()).thenReturn(AmmoType.AmmoTypeEnum.AC);
        when(mockAC5.getRackSize()).thenReturn(5);
        when(mockPPC.getAmmoType()).thenReturn(AmmoType.AmmoTypeEnum.NA);
        when(mockSRM4.getAmmoType()).thenReturn(AmmoType.AmmoTypeEnum.SRM);
        when(mockSRM4.getRackSize()).thenReturn(4);

        when(mockAC5AmmoType.getAmmoType()).thenReturn(AmmoType.AmmoTypeEnum.AC);
        when(mockAC5AmmoType.getRackSize()).thenReturn(5);
        when(mockSRM4AmmoType.getAmmoType()).thenReturn(AmmoType.AmmoTypeEnum.SRM);
        when(mockSRM4AmmoType.getRackSize()).thenReturn(4);
        when(mockSRM4AmmoType.getMunitionType()).thenReturn(EnumSet.of(AmmoType.Munitions.M_STANDARD));
        when(mockInferno4AmmoType.getAmmoType()).thenReturn(AmmoType.AmmoTypeEnum.SRM);
        when(mockInferno4AmmoType.getRackSize()).thenReturn(4);
        when(mockInferno4AmmoType.getMunitionType()).thenReturn(EnumSet.of(AmmoType.Munitions.M_INFERNO));
        when(mockAC10AmmoType.getAmmoType()).thenReturn(AmmoType.AmmoTypeEnum.AC);
        when(mockAC10AmmoType.getRackSize()).thenReturn(10);
        when(mockSRM6AmmoType.getAmmoType()).thenReturn(AmmoType.AmmoTypeEnum.SRM);
        when(mockSRM6AmmoType.getRackSize()).thenReturn(6);
        when(mockSRM6AmmoType.getMunitionType()).thenReturn(EnumSet.of(AmmoType.Munitions.M_STANDARD));

        when(mockAmmoSrm4.getType()).thenReturn(mockSRM4AmmoType);
        when(mockAmmoSrm4.isAmmoUsable()).thenReturn(true);
        when(mockAmmoInferno4.getType()).thenReturn(mockInferno4AmmoType);
        when(mockAmmoInferno4.isAmmoUsable()).thenReturn(true);
        when(mockAmmoSrm6.getType()).thenReturn(mockSRM6AmmoType);
        when(mockAmmoSrm6.isAmmoUsable()).thenReturn(true);
        when(mockAmmoAC5.getType()).thenReturn(mockAC5AmmoType);
        when(mockAmmoAC5.isAmmoUsable()).thenReturn(true);
        when(mockAmmoAC10.getType()).thenReturn(mockAC10AmmoType);
        when(mockAmmoAC10.isAmmoUsable()).thenReturn(true);
        when(mockAmmoAC5Empty.getType()).thenReturn(mockAC5AmmoType);
        when(mockAmmoAC5Empty.isAmmoUsable()).thenReturn(false);

        when(mockNotAmmo.getType()).thenReturn(notAmmoType);

        doReturn(true).when(mockSRM4AmmoType).equalsAmmoTypeOnly(eq(mockSRM4AmmoType));
        doReturn(true).when(mockSRM4AmmoType).equalsAmmoTypeOnly(eq(mockInferno4AmmoType));
        doReturn(true).when(mockInferno4AmmoType).equalsAmmoTypeOnly(eq(mockInferno4AmmoType));
        doReturn(true).when(mockInferno4AmmoType).equalsAmmoTypeOnly(eq(mockSRM4AmmoType));
    }

    @Test
    void testIsAmmoValid() {
        // Test ammo that matches weapon.
        assertTrue(AmmoType.isAmmoValid(mockAmmoAC5, mockAC5));
        assertTrue(AmmoType.isAmmoValid(mockAmmoSrm4, mockSRM4));
        assertTrue(AmmoType.isAmmoValid(mockAmmoInferno4, mockSRM4));

        // Test an empty ammo bin.
        assertFalse(AmmoType.isAmmoValid(mockAmmoAC5Empty, mockAC5));

        // Test null ammo.
        assertFalse(AmmoType.isAmmoValid((AmmoMounted) null, mockAC5));
        assertFalse(AmmoType.isAmmoValid((AmmoType) null, mockAC5));

        // Test incompatible ammo.
        assertFalse(AmmoType.isAmmoValid(mockAmmoAC5, mockSRM4));
        assertFalse(AmmoType.isAmmoValid(mockAmmoSrm4, mockPPC));

        // Test wrong rack size.
        assertFalse(AmmoType.isAmmoValid(mockAmmoAC10, mockAC5));
        assertFalse(AmmoType.isAmmoValid(mockAmmoSrm6, mockSRM4));

        // Test something that's not ammo.
        assertFalse(AmmoType.isAmmoValid(mockNotAmmo, mockAC5));
    }

    @Test
    void testCanSwitchToAmmo() {
        WeaponMounted mockWeapon = mock(WeaponMounted.class);
        when(mockWeapon.getLinkedAmmo()).thenReturn(mockAmmoSrm4);

        assertTrue(AmmoType.canSwitchToAmmo(mockWeapon, mockInferno4AmmoType));

        assertFalse(AmmoType.canSwitchToAmmo(mockWeapon, mockSRM6AmmoType));
    }
}

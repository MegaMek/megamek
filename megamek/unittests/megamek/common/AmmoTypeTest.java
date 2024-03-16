/*
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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 9/21/13 8:13 AM
 */
public class AmmoTypeTest {
    static WeaponType mockAC5 = mock(WeaponType.class);
    static AmmoType mockAC5AmmoType = mock(AmmoType.class);
    static Mounted mockAmmoAC5 = mock(Mounted.class);
    static Mounted mockAmmoAC5Empty = mock(Mounted.class);
    static AmmoType mockAC10AmmoType = mock(AmmoType.class);
    static Mounted mockAmmoAC10 = mock(Mounted.class);

    static WeaponType mockPPC = mock(WeaponType.class);

    static WeaponType mockSRM4 = mock(WeaponType.class);
    static AmmoType mockSRM4AmmoType = mock(AmmoType.class);
    static Mounted mockAmmoSrm4 = mock(Mounted.class);
    static AmmoType mockSRM6AmmoType = mock(AmmoType.class);
    static Mounted mockAmmoSrm6 = mock(Mounted.class);
    static AmmoType mockInferno4AmmoType = mock(AmmoType.class);
    static Mounted mockAmmoInferno4 = mock(Mounted.class);

    static MiscType notAmmoType = mock(MiscType.class);
    static Mounted mockNotAmmo = mock(Mounted.class);

    @BeforeAll
    public static void beforeAll() {
        when(mockAC5.getAmmoType()).thenReturn(AmmoType.T_AC);
        when(mockAC5.getRackSize()).thenReturn(5);
        when(mockPPC.getAmmoType()).thenReturn(AmmoType.T_NA);
        when(mockSRM4.getAmmoType()).thenReturn(AmmoType.T_SRM);
        when(mockSRM4.getRackSize()).thenReturn(4);

        when(mockAC5AmmoType.getAmmoType()).thenReturn(AmmoType.T_AC);
        when(mockAC5AmmoType.getRackSize()).thenReturn(5);
        when(mockSRM4AmmoType.getAmmoType()).thenReturn(AmmoType.T_SRM);
        when(mockSRM4AmmoType.getRackSize()).thenReturn(4);
        when(mockSRM4AmmoType.getMunitionType()).thenReturn(EnumSet.of(AmmoType.Munitions.M_STANDARD));
        when(mockInferno4AmmoType.getAmmoType()).thenReturn(AmmoType.T_SRM);
        when(mockInferno4AmmoType.getRackSize()).thenReturn(4);
        when(mockInferno4AmmoType.getMunitionType()).thenReturn(EnumSet.of(AmmoType.Munitions.M_INFERNO));
        when(mockAC10AmmoType.getAmmoType()).thenReturn(AmmoType.T_AC);
        when(mockAC10AmmoType.getRackSize()).thenReturn(10);
        when(mockSRM6AmmoType.getAmmoType()).thenReturn(AmmoType.T_SRM);
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
    public void testIsAmmoValid() {
        // Test ammo that matches weapon.
        assertTrue(AmmoType.isAmmoValid(mockAmmoAC5, mockAC5));
        assertTrue(AmmoType.isAmmoValid(mockAmmoSrm4, mockSRM4));
        assertTrue(AmmoType.isAmmoValid(mockAmmoInferno4, mockSRM4));

        // Test an empty ammo bin.
        assertFalse(AmmoType.isAmmoValid(mockAmmoAC5Empty, mockAC5));

        // Test null ammo.
        assertFalse(AmmoType.isAmmoValid((Mounted) null, mockAC5));
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
    public void testCanSwitchToAmmo() {
        Mounted mockWeapon = mock(Mounted.class);
        when(mockWeapon.getLinked()).thenReturn(mockAmmoSrm4);

        assertTrue(AmmoType.canSwitchToAmmo(mockWeapon, mockInferno4AmmoType));

        assertFalse(AmmoType.canSwitchToAmmo(mockWeapon, mockSRM6AmmoType));
    }
}

/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

/**
 * Created with IntelliJ IDEA.
 *
 * @version %Id%
 * @lastEditBy Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 9/21/13 8:13 AM
 */
@RunWith(JUnit4.class)
public class AmmoTypeTest {

    WeaponType mockAC5 = Mockito.mock(WeaponType.class);
    AmmoType mockAC5AmmoType = Mockito.mock(AmmoType.class);
    Mounted mockAmmoAC5 = Mockito.mock(Mounted.class);
    Mounted mockAmmoAC5Empty = Mockito.mock(Mounted.class);
    AmmoType mockAC10AmmoType = Mockito.mock(AmmoType.class);
    Mounted mockAmmoAC10 = Mockito.mock(Mounted.class);

    WeaponType mockPPC = Mockito.mock(WeaponType.class);

    WeaponType mockSRM4 = Mockito.mock(WeaponType.class);
    AmmoType mockSRM4AmmoType = Mockito.mock(AmmoType.class);
    Mounted mockAmmoSrm4 = Mockito.mock(Mounted.class);
    AmmoType mockSRM6AmmoType = Mockito.mock(AmmoType.class);
    Mounted mockAmmoSrm6 = Mockito.mock(Mounted.class);
    AmmoType mockInferno4AmmoType = Mockito.mock(AmmoType.class);
    Mounted mockAmmoInferno4 = Mockito.mock(Mounted.class);

    MiscType notAmmoType = Mockito.mock(MiscType.class);
    Mounted mockNotAmmo = Mockito.mock(Mounted.class);

    @Before
    public void setUp() {

        Mockito.when(mockAC5.getAmmoType()).thenReturn(AmmoType.T_AC);
        Mockito.when(mockAC5.getRackSize()).thenReturn(5);
        Mockito.when(mockPPC.getAmmoType()).thenReturn(AmmoType.T_NA);
        Mockito.when(mockSRM4.getAmmoType()).thenReturn(AmmoType.T_SRM);
        Mockito.when(mockSRM4.getRackSize()).thenReturn(4);

        Mockito.when(mockAC5AmmoType.getAmmoType()).thenReturn(AmmoType.T_AC);
        Mockito.when(mockAC5AmmoType.getRackSize()).thenReturn(5);
        Mockito.when(mockSRM4AmmoType.getAmmoType()).thenReturn(AmmoType.T_SRM);
        Mockito.when(mockSRM4AmmoType.getRackSize()).thenReturn(4);
        Mockito.when(mockInferno4AmmoType.getAmmoType()).thenReturn(AmmoType.T_SRM);
        Mockito.when(mockInferno4AmmoType.getRackSize()).thenReturn(4);
        Mockito.when(mockAC10AmmoType.getAmmoType()).thenReturn(AmmoType.T_AC);
        Mockito.when(mockAC10AmmoType.getRackSize()).thenReturn(10);
        Mockito.when(mockSRM6AmmoType.getAmmoType()).thenReturn(AmmoType.T_SRM);
        Mockito.when(mockSRM6AmmoType.getRackSize()).thenReturn(6);

        Mockito.when(mockAmmoSrm4.getType()).thenReturn(mockSRM4AmmoType);
        Mockito.when(mockAmmoSrm4.isAmmoUsable()).thenReturn(true);
        Mockito.when(mockAmmoInferno4.getType()).thenReturn(mockInferno4AmmoType);
        Mockito.when(mockAmmoInferno4.isAmmoUsable()).thenReturn(true);
        Mockito.when(mockAmmoSrm6.getType()).thenReturn(mockSRM6AmmoType);
        Mockito.when(mockAmmoSrm6.isAmmoUsable()).thenReturn(true);
        Mockito.when(mockAmmoAC5.getType()).thenReturn(mockAC5AmmoType);
        Mockito.when(mockAmmoAC5.isAmmoUsable()).thenReturn(true);
        Mockito.when(mockAmmoAC10.getType()).thenReturn(mockAC10AmmoType);
        Mockito.when(mockAmmoAC10.isAmmoUsable()).thenReturn(true);
        Mockito.when(mockAmmoAC5Empty.getType()).thenReturn(mockAC5AmmoType);
        Mockito.when(mockAmmoAC5Empty.isAmmoUsable()).thenReturn(false);

        Mockito.when(mockNotAmmo.getType()).thenReturn(notAmmoType);
    }

    @Test
    public void testIsAmmoValid() {
        // Test ammo that matches weapon.
        Assert.assertTrue(AmmoType.isAmmoValid(mockAmmoAC5, mockAC5));
        Assert.assertTrue(AmmoType.isAmmoValid(mockAmmoSrm4, mockSRM4));
        Assert.assertTrue(AmmoType.isAmmoValid(mockAmmoInferno4, mockSRM4));

        // Test an empty ammo bin.
        Assert.assertFalse(AmmoType.isAmmoValid(mockAmmoAC5Empty, mockAC5));

        // Test null ammo.
        Assert.assertFalse(AmmoType.isAmmoValid((Mounted) null, mockAC5));
        Assert.assertFalse(AmmoType.isAmmoValid((AmmoType) null, mockAC5));

        // Test incompatible ammo.
        Assert.assertFalse(AmmoType.isAmmoValid(mockAmmoAC5, mockSRM4));
        Assert.assertFalse(AmmoType.isAmmoValid(mockAmmoSrm4, mockPPC));

        // Test wrong rack size.
        Assert.assertFalse(AmmoType.isAmmoValid(mockAmmoAC10, mockAC5));
        Assert.assertFalse(AmmoType.isAmmoValid(mockAmmoSrm6, mockSRM4));

        // Test something that's not ammo.
        Assert.assertFalse(AmmoType.isAmmoValid(mockNotAmmo, mockAC5));
    }
}

/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
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
package megamek.client.bot.princess;

import junit.framework.TestCase;
import megamek.common.AmmoType;
import megamek.common.BipedMech;
import megamek.common.BuildingTarget;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Infantry;
import megamek.common.Mounted;
import megamek.common.Tank;
import megamek.common.Targetable;
import megamek.common.VTOL;
import megamek.common.WeaponType;
import megamek.common.weapons.ATMWeapon;
import megamek.common.weapons.ISMG;
import megamek.common.weapons.ISMediumLaser;
import megamek.common.weapons.LRMWeapon;
import megamek.common.weapons.MMLWeapon;
import megamek.common.weapons.SRMWeapon;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @version %Id%
 * @lastEditBy Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 9/21/13 8:55 AM
 */
@RunWith(JUnit4.class)
public class FireControlTest {

    // AC5
    private WeaponType mockWeaponTypeAC5 = Mockito.mock(WeaponType.class);
    private AmmoType mockAmmoTypeAC5Std = Mockito.mock(AmmoType.class);
    private Mounted mockAmmoAC5Std = Mockito.mock(Mounted.class);
    private AmmoType mockAmmoTypeAC5Flak = Mockito.mock(AmmoType.class);
    private Mounted mockAmmoAC5Flak = Mockito.mock(Mounted.class);
    private AmmoType mockAmmoTypeAC5Incendiary = Mockito.mock(AmmoType.class);
    private Mounted mockAmmoAc5Incendiary = Mockito.mock(Mounted.class);
    private AmmoType mockAmmoTypeAc5Flechette = Mockito.mock(AmmoType.class);
    private Mounted mockAmmoAc5Flechette = Mockito.mock(Mounted.class);

    // LB10X
    private WeaponType mockLB10X = Mockito.mock(WeaponType.class);
    private AmmoType mockAmmoTypeLB10XSlug = Mockito.mock(AmmoType.class);
    private Mounted mockAmmoLB10XSlug = Mockito.mock(Mounted.class);
    private AmmoType mockAmmoTypeLB10XCluster = Mockito.mock(AmmoType.class);
    private Mounted mockAmmoLB10XCluster = Mockito.mock(Mounted.class);

    // MML
    private WeaponType mockMML5 = Mockito.mock(MMLWeapon.class);
    private AmmoType mockAmmoTypeSRM5 = Mockito.mock(AmmoType.class);
    private Mounted mockAmmoSRM5 = Mockito.mock(Mounted.class);
    private AmmoType mockAmmoTypeLRM5 = Mockito.mock(AmmoType.class);
    private Mounted mockAmmoLRM5 = Mockito.mock(Mounted.class);
    private AmmoType mockAmmoTypeInferno5 = Mockito.mock(AmmoType.class);
    private Mounted mockAmmoInfero5 = Mockito.mock(Mounted.class);
    private AmmoType mockAmmoTypeLrm5Frag = Mockito.mock(AmmoType.class);
    private Mounted mockAmmoLrm5Frag = Mockito.mock(Mounted.class);

    // ATM
    WeaponType mockAtm5 = Mockito.mock(ATMWeapon.class);
    AmmoType mockAmmoTypeAtm5He = Mockito.mock(AmmoType.class);
    Mounted mockAmmoAtm5He = Mockito.mock(Mounted.class);
    AmmoType mockAmmoTypeAtm5St = Mockito.mock(AmmoType.class);
    Mounted mockAmmoAtm5St = Mockito.mock(Mounted.class);
    AmmoType mockAmmoTypeAtm5Er = Mockito.mock(AmmoType.class);
    Mounted mockAmmoAtm5Er = Mockito.mock(Mounted.class);

    @Before
    public void setUp() {
        // AC5
        Mockito.when(mockWeaponTypeAC5.getAmmoType()).thenReturn(AmmoType.T_AC);
        Mockito.when(mockAmmoTypeAC5Std.getAmmoType()).thenReturn(AmmoType.T_AC);
        Mockito.when(mockAmmoTypeAC5Std.getMunitionType()).thenReturn(AmmoType.M_STANDARD);
        Mockito.when(mockAmmoAC5Std.getType()).thenReturn(mockAmmoTypeAC5Std);
        Mockito.when(mockAmmoAC5Std.isAmmoUsable()).thenReturn(true);
        Mockito.when(mockAmmoTypeAC5Flak.getAmmoType()).thenReturn(AmmoType.T_AC);
        Mockito.when(mockAmmoTypeAC5Flak.getMunitionType()).thenReturn(AmmoType.M_FLAK);
        Mockito.when(mockAmmoAC5Flak.getType()).thenReturn(mockAmmoTypeAC5Flak);
        Mockito.when(mockAmmoAC5Flak.isAmmoUsable()).thenReturn(true);
        Mockito.when(mockAmmoTypeAC5Incendiary.getMunitionType()).thenReturn(AmmoType.M_INCENDIARY_AC);
        Mockito.when(mockAmmoTypeAC5Incendiary.getAmmoType()).thenReturn(AmmoType.T_AC);
        Mockito.when(mockAmmoAc5Incendiary.getType()).thenReturn(mockAmmoTypeAC5Incendiary);
        Mockito.when(mockAmmoAc5Incendiary.isAmmoUsable()).thenReturn(true);
        Mockito.when(mockAmmoTypeAc5Flechette.getAmmoType()).thenReturn(AmmoType.T_AC);
        Mockito.when(mockAmmoTypeAc5Flechette.getMunitionType()).thenReturn(AmmoType.M_FLECHETTE);
        Mockito.when(mockAmmoAc5Flechette.getType()).thenReturn(mockAmmoTypeAc5Flechette);
        Mockito.when(mockAmmoAc5Flechette.isAmmoUsable()).thenReturn(true);

        // LB10X
        Mockito.when(mockLB10X.getAmmoType()).thenReturn(AmmoType.T_AC_LBX);
        Mockito.when(mockAmmoTypeLB10XSlug.getAmmoType()).thenReturn(AmmoType.T_AC_LBX);
        Mockito.when(mockAmmoTypeLB10XSlug.getMunitionType()).thenReturn(AmmoType.M_STANDARD);
        Mockito.when(mockAmmoLB10XSlug.getType()).thenReturn(mockAmmoTypeLB10XSlug);
        Mockito.when(mockAmmoLB10XSlug.isAmmoUsable()).thenReturn(true);
        Mockito.when(mockAmmoTypeLB10XCluster.getAmmoType()).thenReturn(AmmoType.T_AC_LBX);
        Mockito.when(mockAmmoTypeLB10XCluster.getMunitionType()).thenReturn(AmmoType.M_CLUSTER);
        Mockito.when(mockAmmoLB10XCluster.getType()).thenReturn(mockAmmoTypeLB10XCluster);
        Mockito.when(mockAmmoLB10XCluster.isAmmoUsable()).thenReturn(true);

        // MML
        Mockito.when(mockMML5.getAmmoType()).thenReturn(AmmoType.T_MML);
        Mockito.when(mockAmmoTypeSRM5.getMunitionType()).thenReturn(AmmoType.M_STANDARD);
        Mockito.when(mockAmmoTypeSRM5.getAmmoType()).thenReturn(AmmoType.T_MML);
        Mockito.when(mockAmmoSRM5.getType()).thenReturn(mockAmmoTypeSRM5);
        Mockito.when(mockAmmoSRM5.isAmmoUsable()).thenReturn(true);
        Mockito.when(mockAmmoTypeLRM5.getMunitionType()).thenReturn(AmmoType.M_STANDARD);
        Mockito.when(mockAmmoTypeLRM5.hasFlag(Mockito.any(BigInteger.class))).thenReturn(false);
        Mockito.when(mockAmmoTypeLRM5.hasFlag(Mockito.eq(AmmoType.F_MML_LRM))).thenReturn(true);
        Mockito.when(mockAmmoTypeLRM5.getAmmoType()).thenReturn(AmmoType.T_MML);
        Mockito.when(mockAmmoLRM5.getType()).thenReturn(mockAmmoTypeLRM5);
        Mockito.when(mockAmmoLRM5.isAmmoUsable()).thenReturn(true);
        Mockito.when(mockAmmoTypeInferno5.getMunitionType()).thenReturn(AmmoType.M_INFERNO);
        Mockito.when(mockAmmoTypeInferno5.getAmmoType()).thenReturn(AmmoType.T_MML);
        Mockito.when(mockAmmoInfero5.getType()).thenReturn(mockAmmoTypeInferno5);
        Mockito.when(mockAmmoInfero5.isAmmoUsable()).thenReturn(true);
        Mockito.when(mockAmmoTypeLrm5Frag.getMunitionType()).thenReturn(AmmoType.M_FRAGMENTATION);
        Mockito.when(mockAmmoTypeLrm5Frag.hasFlag(Mockito.eq(AmmoType.F_MML_LRM))).thenReturn(true);
        Mockito.when(mockAmmoTypeLrm5Frag.getAmmoType()).thenReturn(AmmoType.T_MML);
        Mockito.when(mockAmmoLrm5Frag.getType()).thenReturn(mockAmmoTypeLrm5Frag);
        Mockito.when(mockAmmoLrm5Frag.isAmmoUsable()).thenReturn(true);

        // ATM
        Mockito.when(mockAtm5.getAmmoType()).thenReturn(AmmoType.T_ATM);
        Mockito.when(mockAtm5.getRackSize()).thenReturn(5);
        Mockito.when(mockAmmoTypeAtm5He.getAmmoType()).thenReturn(AmmoType.T_ATM);
        Mockito.when(mockAmmoTypeAtm5He.getMunitionType()).thenReturn(AmmoType.M_HIGH_EXPLOSIVE);
        Mockito.when(mockAmmoTypeAtm5He.getRackSize()).thenReturn(5);
        Mockito.when(mockAmmoAtm5He.getType()).thenReturn(mockAmmoTypeAtm5He);
        Mockito.when(mockAmmoAtm5He.isAmmoUsable()).thenReturn(true);
        Mockito.when(mockAmmoTypeAtm5St.getMunitionType()).thenReturn(AmmoType.M_STANDARD);
        Mockito.when(mockAmmoTypeAtm5St.getAmmoType()).thenReturn(AmmoType.T_ATM);
        Mockito.when(mockAmmoTypeAtm5St.getRackSize()).thenReturn(5);
        Mockito.when(mockAmmoAtm5St.getType()).thenReturn(mockAmmoTypeAtm5St);
        Mockito.when(mockAmmoAtm5St.isAmmoUsable()).thenReturn(true);
        Mockito.when(mockAmmoTypeAtm5Er.getMunitionType()).thenReturn(AmmoType.M_EXTENDED_RANGE);
        Mockito.when(mockAmmoTypeAtm5Er.getAmmoType()).thenReturn(AmmoType.T_ATM);
        Mockito.when(mockAmmoTypeAtm5Er.getRackSize()).thenReturn(5);
        Mockito.when(mockAmmoAtm5Er.getType()).thenReturn(mockAmmoTypeAtm5Er);
        Mockito.when(mockAmmoAtm5Er.isAmmoUsable()).thenReturn(true);
    }

    @Test
    public void testGetAntiAirAmmo() {

        // Test an ammo list with only 1 bin.
        List<Mounted> testAmmoList = new ArrayList<Mounted>(2);
        testAmmoList.add(mockAmmoAC5Std);
        FireControl testFireControl = new FireControl();
        TestCase.assertNull(testFireControl.getAntiAirAmmo(testAmmoList, mockWeaponTypeAC5, 5));

        // Add the flak ammo.
        testAmmoList.add(mockAmmoAC5Flak);
        TestCase.assertEquals(mockAmmoAC5Flak, testFireControl.getAntiAirAmmo(testAmmoList, mockWeaponTypeAC5, 5));

        // Test a list with 2 bins of standard and 0 flak ammo.
        testAmmoList = new ArrayList<Mounted>(2);
        testAmmoList.add(mockAmmoAC5Std);
        testAmmoList.add(mockAmmoAC5Std);
        TestCase.assertNull(testFireControl.getAntiAirAmmo(testAmmoList, mockWeaponTypeAC5, 5));

        // Test LBX weaponry.
        testAmmoList = new ArrayList<Mounted>(2);
        testAmmoList.add(mockAmmoLB10XCluster);
        testAmmoList.add(mockAmmoLB10XSlug);
        TestCase.assertEquals(mockAmmoLB10XCluster, testFireControl.getAntiAirAmmo(testAmmoList, mockLB10X, 5));
    }

    @Test
    public void testGetHardTargetAmmo() {

        // Test an ammo list with only 1 bin of standard ammo.
        List<Mounted> testAmmoList = new ArrayList<Mounted>(1);
        testAmmoList.add(mockAmmoAC5Std);
        FireControl testFireControl = new FireControl();
        TestCase.assertEquals(mockAmmoAC5Std, testFireControl.getHardTargetAmmo(testAmmoList, mockWeaponTypeAC5, 5));

        // Test an ammo list with only 1 bin of flak ammo.
        testAmmoList = new ArrayList<Mounted>(1);
        testAmmoList.add(mockAmmoAC5Flak);
        TestCase.assertNull(testFireControl.getHardTargetAmmo(testAmmoList, mockWeaponTypeAC5, 5));

        // Test an ammo list with 1 each of standard and flak.
        testAmmoList = new ArrayList<Mounted>(2);
        testAmmoList.add(mockAmmoAC5Flak);
        testAmmoList.add(mockAmmoAC5Std);
        TestCase.assertEquals(mockAmmoAC5Std, testFireControl.getHardTargetAmmo(testAmmoList, mockWeaponTypeAC5, 5));

        // Test LBX weaponry.
        testAmmoList = new ArrayList<Mounted>(2);
        testAmmoList.add(mockAmmoLB10XCluster);
        testAmmoList.add(mockAmmoLB10XSlug);
        TestCase.assertEquals(mockAmmoLB10XSlug, testFireControl.getHardTargetAmmo(testAmmoList, mockLB10X, 5));

        // Test MMLs
        testAmmoList = new ArrayList<Mounted>(3);
        testAmmoList.add(mockAmmoLRM5);
        testAmmoList.add(mockAmmoSRM5);
        testAmmoList.add(mockAmmoInfero5);
        TestCase.assertEquals(mockAmmoSRM5, testFireControl.getHardTargetAmmo(testAmmoList, mockMML5, 4));
        TestCase.assertEquals(mockAmmoLRM5, testFireControl.getHardTargetAmmo(testAmmoList, mockMML5, 8));
        TestCase.assertEquals(mockAmmoLRM5, testFireControl.getHardTargetAmmo(testAmmoList, mockMML5, 10));

        // Test MMLs without LRMs.
        testAmmoList = new ArrayList<Mounted>(2);
        testAmmoList.add(mockAmmoSRM5);
        testAmmoList.add(mockAmmoInfero5);
        TestCase.assertEquals(mockAmmoSRM5, testFireControl.getHardTargetAmmo(testAmmoList, mockMML5, 4));
        TestCase.assertEquals(mockAmmoSRM5, testFireControl.getHardTargetAmmo(testAmmoList, mockMML5, 8));
        TestCase.assertNull(testFireControl.getHardTargetAmmo(testAmmoList, mockMML5, 10));

        // Test MMLs without SRMs.
        testAmmoList = new ArrayList<Mounted>(2);
        testAmmoList.add(mockAmmoLRM5);
        testAmmoList.add(mockAmmoInfero5);
        TestCase.assertEquals(mockAmmoLRM5, testFireControl.getHardTargetAmmo(testAmmoList, mockMML5, 4));
        TestCase.assertEquals(mockAmmoLRM5, testFireControl.getHardTargetAmmo(testAmmoList, mockMML5, 8));
        TestCase.assertEquals(mockAmmoLRM5, testFireControl.getHardTargetAmmo(testAmmoList, mockMML5, 10));
    }

    @Test
    public void testGetClusterAmmo() {

        // Test an ammo list with only 1 bin of cluster ammo.
        List<Mounted> testAmmoList = new ArrayList<Mounted>(2);
        testAmmoList.add(mockAmmoLB10XCluster);
        FireControl testFireControl = new FireControl();
        TestCase.assertEquals(mockAmmoLB10XCluster, testFireControl.getClusterAmmo(testAmmoList, mockLB10X, 5));

        // Test an ammo list with only 1 bin of slug ammo.
        testAmmoList = new ArrayList<Mounted>(2);
        testAmmoList.add(mockAmmoLB10XSlug);
        testFireControl = new FireControl();
        TestCase.assertNull(testFireControl.getClusterAmmo(testAmmoList, mockLB10X, 5));

        // Test with both loaded
        testAmmoList = new ArrayList<Mounted>(2);
        testAmmoList.add(mockAmmoLB10XCluster);
        testAmmoList.add(mockAmmoLB10XSlug);
        TestCase.assertEquals(mockAmmoLB10XCluster, testFireControl.getClusterAmmo(testAmmoList, mockLB10X, 5));
    }

    @Test
    public void testGetHeatAmmo() {

        // Test an ammo list with only 1 bin of incendiary ammo.
        List<Mounted> testAmmoList = new ArrayList<Mounted>(1);
        testAmmoList.add(mockAmmoAc5Incendiary);
        FireControl testFireControl = new FireControl();
        TestCase.assertEquals(mockAmmoAc5Incendiary, testFireControl.getHeatAmmo(testAmmoList, mockWeaponTypeAC5, 5));

        // Test an ammo list with only 1 bin of standard ammo.
        testAmmoList = new ArrayList<Mounted>(1);
        testAmmoList.add(mockAmmoAC5Std);
        TestCase.assertNull(testFireControl.getHeatAmmo(testAmmoList, mockWeaponTypeAC5, 5));

        // Test a list with multiple types of ammo.
        testAmmoList = new ArrayList<Mounted>(3);
        testAmmoList.add(mockAmmoAC5Std);
        testAmmoList.add(mockAmmoAc5Incendiary);
        testAmmoList.add(mockAmmoAC5Flak);
        TestCase.assertEquals(mockAmmoAc5Incendiary, testFireControl.getHeatAmmo(testAmmoList, mockWeaponTypeAC5, 5));

        // Test LBX
        testAmmoList = new ArrayList<Mounted>(2);
        testAmmoList.add(mockAmmoLB10XCluster);
        testAmmoList.add(mockAmmoLB10XSlug);
        TestCase.assertNull(testFireControl.getHeatAmmo(testAmmoList, mockLB10X, 5));

        // Test MMLs
        testAmmoList = new ArrayList<Mounted>(3);
        testAmmoList.add(mockAmmoLRM5);
        testAmmoList.add(mockAmmoSRM5);
        testAmmoList.add(mockAmmoInfero5);
        TestCase.assertEquals(mockAmmoInfero5, testFireControl.getHeatAmmo(testAmmoList, mockMML5, 4));
        TestCase.assertEquals(mockAmmoInfero5, testFireControl.getHeatAmmo(testAmmoList, mockMML5, 8));
        TestCase.assertNull(testFireControl.getHeatAmmo(testAmmoList, mockMML5, 10));
    }

    @Test
    public void testGetAntiInfantryAmmo() {

        // Test an ammo list with only 1 bin of flechette ammo.
        List<Mounted> testAmmoList = new ArrayList<Mounted>(1);
        testAmmoList.add(mockAmmoAc5Flechette);
        FireControl testFireControl = new FireControl();
        TestCase.assertEquals(mockAmmoAc5Flechette, testFireControl.getAntiInfantryAmmo(testAmmoList, mockWeaponTypeAC5, 5));

        // Test an ammo list with only 1 bin of standard ammo.
        testAmmoList = new ArrayList<Mounted>(1);
        testAmmoList.add(mockAmmoAC5Std);
        TestCase.assertNull(testFireControl.getAntiInfantryAmmo(testAmmoList, mockWeaponTypeAC5, 5));

        // Test a list with multiple types of ammo.
        testAmmoList = new ArrayList<Mounted>(3);
        testAmmoList.add(mockAmmoAC5Std);
        testAmmoList.add(mockAmmoAC5Flak);
        testAmmoList.add(mockAmmoAc5Flechette);
        TestCase.assertEquals(mockAmmoAc5Flechette, testFireControl.getAntiInfantryAmmo(testAmmoList, mockWeaponTypeAC5, 5));

        // Test LBX
        testAmmoList = new ArrayList<Mounted>(2);
        testAmmoList.add(mockAmmoLB10XCluster);
        testAmmoList.add(mockAmmoLB10XSlug);
        TestCase.assertEquals(mockAmmoLB10XCluster, testFireControl.getAntiInfantryAmmo(testAmmoList, mockLB10X, 5));

        // Test MMLs
        testAmmoList = new ArrayList<Mounted>(4);
        testAmmoList.add(mockAmmoLRM5);
        testAmmoList.add(mockAmmoSRM5);
        testAmmoList.add(mockAmmoInfero5);
        testAmmoList.add(mockAmmoLrm5Frag);
        TestCase.assertEquals(mockAmmoInfero5, testFireControl.getAntiInfantryAmmo(testAmmoList, mockMML5, 4));
        TestCase.assertEquals(mockAmmoLrm5Frag, testFireControl.getAntiInfantryAmmo(testAmmoList, mockMML5, 8));
        TestCase.assertEquals(mockAmmoLrm5Frag, testFireControl.getAntiInfantryAmmo(testAmmoList, mockMML5, 10));
    }

    @Test
    public void testGetAntiVeeAmmo() {

        // Test an ammo list with only 1 bin of standard ammo.
        List<Mounted> testAmmoList = new ArrayList<Mounted>(1);
        testAmmoList.add(mockAmmoAC5Std);
        FireControl testFireControl = new FireControl();
        TestCase.assertNull(testFireControl.getAntiVeeAmmo(testAmmoList, mockWeaponTypeAC5, 5));

        // Test an ammo list with only 1 bin of incendiary ammo.
        testAmmoList = new ArrayList<Mounted>(1);
        testAmmoList.add(mockAmmoAc5Incendiary);
        testFireControl = new FireControl();
        TestCase.assertEquals(mockAmmoAc5Incendiary, testFireControl.getAntiVeeAmmo(testAmmoList, mockWeaponTypeAC5, 5));

        // Test a list with multiple types of ammo.
        testAmmoList = new ArrayList<Mounted>(3);
        testAmmoList.add(mockAmmoAC5Std);
        testAmmoList.add(mockAmmoAc5Incendiary);
        testAmmoList.add(mockAmmoAC5Flak);
        TestCase.assertEquals(mockAmmoAc5Incendiary, testFireControl.getAntiVeeAmmo(testAmmoList, mockWeaponTypeAC5, 5));

        // Test LBX
        testAmmoList = new ArrayList<Mounted>(2);
        testAmmoList.add(mockAmmoLB10XCluster);
        testAmmoList.add(mockAmmoLB10XSlug);
        TestCase.assertEquals(mockAmmoLB10XCluster, testFireControl.getAntiVeeAmmo(testAmmoList, mockLB10X, 5));

        // Test MMLs
        testAmmoList = new ArrayList<Mounted>(4);
        testAmmoList.add(mockAmmoLRM5);
        testAmmoList.add(mockAmmoSRM5);
        testAmmoList.add(mockAmmoInfero5);
        testAmmoList.add(mockAmmoLrm5Frag);
        TestCase.assertEquals(mockAmmoInfero5, testFireControl.getAntiVeeAmmo(testAmmoList, mockMML5, 4));
        TestCase.assertEquals(mockAmmoInfero5, testFireControl.getAntiVeeAmmo(testAmmoList, mockMML5, 8));
        TestCase.assertNull(testFireControl.getAntiVeeAmmo(testAmmoList, mockMML5, 10));
    }

    @Test
    public void testGetAtmAmmo() {

        // Test a list with just HE ammo.
        List<Mounted> testAmmoList = new ArrayList<Mounted>(1);
        testAmmoList.add(mockAmmoAtm5He);
        FireControl testFireControl = new FireControl();
        TestCase.assertEquals(mockAmmoAtm5He, testFireControl.getAtmAmmo(testAmmoList, 5));
        TestCase.assertNull(testFireControl.getAtmAmmo(testAmmoList, 15));

        // Test a list with just Standard ammo.
        testAmmoList = new ArrayList<Mounted>(1);
        testAmmoList.add(mockAmmoAtm5St);
        TestCase.assertEquals(mockAmmoAtm5St, testFireControl.getAtmAmmo(testAmmoList, 5));
        TestCase.assertNull(testFireControl.getAtmAmmo(testAmmoList, 20));

        // Test a list with just ER ammo.
        testAmmoList = new ArrayList<Mounted>(1);
        testAmmoList.add(mockAmmoAtm5Er);
        TestCase.assertEquals(mockAmmoAtm5Er, testFireControl.getAtmAmmo(testAmmoList, 5));

        // Test a list with all 3 ammo types
        testAmmoList = new ArrayList<Mounted>(3);
        testAmmoList.add(mockAmmoAtm5He);
        testAmmoList.add(mockAmmoAtm5Er);
        testAmmoList.add(mockAmmoAtm5St);
        TestCase.assertEquals(mockAmmoAtm5Er, testFireControl.getAtmAmmo(testAmmoList, 20));
        TestCase.assertEquals(mockAmmoAtm5Er, testFireControl.getAtmAmmo(testAmmoList, 12));
        TestCase.assertEquals(mockAmmoAtm5St, testFireControl.getAtmAmmo(testAmmoList, 8));
        TestCase.assertEquals(mockAmmoAtm5He, testFireControl.getAtmAmmo(testAmmoList, 6));
        TestCase.assertEquals(mockAmmoAtm5St, testFireControl.getAtmAmmo(testAmmoList, 5));
        TestCase.assertEquals(mockAmmoAtm5He, testFireControl.getAtmAmmo(testAmmoList, 3));

        // Test a list with just HE and Standard ammo types.
        testAmmoList = new ArrayList<Mounted>(2);
        testAmmoList.add(mockAmmoAtm5He);
        testAmmoList.add(mockAmmoAtm5St);
        TestCase.assertNull(testFireControl.getAtmAmmo(testAmmoList, 20));
        TestCase.assertEquals(mockAmmoAtm5St, testFireControl.getAtmAmmo(testAmmoList, 12));
        TestCase.assertEquals(mockAmmoAtm5St, testFireControl.getAtmAmmo(testAmmoList, 8));
        TestCase.assertEquals(mockAmmoAtm5He, testFireControl.getAtmAmmo(testAmmoList, 6));
        TestCase.assertEquals(mockAmmoAtm5St, testFireControl.getAtmAmmo(testAmmoList, 5));
        TestCase.assertEquals(mockAmmoAtm5He, testFireControl.getAtmAmmo(testAmmoList, 3));

        // Test a list with just HE and ER ammo types.
        testAmmoList = new ArrayList<Mounted>(2);
        testAmmoList.add(mockAmmoAtm5He);
        testAmmoList.add(mockAmmoAtm5Er);
        TestCase.assertEquals(mockAmmoAtm5Er, testFireControl.getAtmAmmo(testAmmoList, 20));
        TestCase.assertEquals(mockAmmoAtm5Er, testFireControl.getAtmAmmo(testAmmoList, 12));
        TestCase.assertEquals(mockAmmoAtm5Er, testFireControl.getAtmAmmo(testAmmoList, 8));
        TestCase.assertEquals(mockAmmoAtm5He, testFireControl.getAtmAmmo(testAmmoList, 6));
        TestCase.assertEquals(mockAmmoAtm5He, testFireControl.getAtmAmmo(testAmmoList, 5));
        TestCase.assertEquals(mockAmmoAtm5He, testFireControl.getAtmAmmo(testAmmoList, 3));

        // Test a list with just Standard and ER ammo types.
        testAmmoList = new ArrayList<Mounted>(2);
        testAmmoList.add(mockAmmoAtm5St);
        testAmmoList.add(mockAmmoAtm5Er);
        TestCase.assertEquals(mockAmmoAtm5Er, testFireControl.getAtmAmmo(testAmmoList, 20));
        TestCase.assertEquals(mockAmmoAtm5Er, testFireControl.getAtmAmmo(testAmmoList, 12));
        TestCase.assertEquals(mockAmmoAtm5St, testFireControl.getAtmAmmo(testAmmoList, 8));
        TestCase.assertEquals(mockAmmoAtm5St, testFireControl.getAtmAmmo(testAmmoList, 6));
        TestCase.assertEquals(mockAmmoAtm5St, testFireControl.getAtmAmmo(testAmmoList, 5));
        TestCase.assertEquals(mockAmmoAtm5St, testFireControl.getAtmAmmo(testAmmoList, 3));
    }

    @Test
    public void testGetGeneralMmlAmmo() {

        // Test a list with just SRM ammo.
        List<Mounted> testAmmoList = new ArrayList<Mounted>(1);
        testAmmoList.add(mockAmmoSRM5);
        FireControl testFireControl = new FireControl();
        TestCase.assertEquals(mockAmmoSRM5, testFireControl.getGeneralMmlAmmo(testAmmoList, 6));
        TestCase.assertNull(testFireControl.getGeneralMmlAmmo(testAmmoList, 10));

        // Test a list with just LRM ammo.
        testAmmoList = new ArrayList<Mounted>(1);
        testAmmoList.add(mockAmmoLRM5);
        TestCase.assertEquals(mockAmmoLRM5, testFireControl.getGeneralMmlAmmo(testAmmoList, 10));

        // Test a list with both types of ammo.
        testAmmoList = new ArrayList<Mounted>(2);
        testAmmoList.add(mockAmmoLRM5);
        testAmmoList.add(mockAmmoSRM5);
        TestCase.assertEquals(mockAmmoLRM5, testFireControl.getGeneralMmlAmmo(testAmmoList, 10));
        TestCase.assertEquals(mockAmmoLRM5, testFireControl.getGeneralMmlAmmo(testAmmoList, 6));
        TestCase.assertEquals(mockAmmoSRM5, testFireControl.getGeneralMmlAmmo(testAmmoList, 4));
    }

    @Test
    public void testGetPreferredAmmo() {
        Entity mockShooter = Mockito.mock(BipedMech.class);
        Targetable mockTarget = Mockito.mock(BipedMech.class);
        FireControl testFireControl = new FireControl();

        ArrayList<Mounted> testAmmoList = new ArrayList<Mounted>(5);
        testAmmoList.add(mockAmmoAtm5He);
        testAmmoList.add(mockAmmoAtm5Er);
        testAmmoList.add(mockAmmoAtm5St);
        testAmmoList.add(mockAmmoAC5Std);
        testAmmoList.add(mockAmmoAC5Flak);
        testAmmoList.add(mockAmmoAc5Flechette);
        testAmmoList.add(mockAmmoAc5Incendiary);
        testAmmoList.add(mockAmmoLB10XCluster);
        testAmmoList.add(mockAmmoLB10XSlug);
        testAmmoList.add(mockAmmoSRM5);
        testAmmoList.add(mockAmmoInfero5);
        testAmmoList.add(mockAmmoLRM5);
        Mockito.when(mockShooter.getAmmo()).thenReturn(testAmmoList);
        Mockito.when(mockShooter.getPosition()).thenReturn(new Coords(10, 10));

        // Test ATMs
        Mockito.when(mockTarget.getPosition()).thenReturn(new Coords(10, 30));
        Mockito.when(mockTarget.getPosition()).thenReturn(new Coords(10, 30));
        TestCase.assertEquals(mockAmmoAtm5Er, testFireControl.getPreferredAmmo(mockShooter, mockTarget, mockAtm5));
        Mockito.when(mockTarget.getPosition()).thenReturn(new Coords(10, 22));
        TestCase.assertEquals(mockAmmoAtm5Er, testFireControl.getPreferredAmmo(mockShooter, mockTarget, mockAtm5));
        Mockito.when(mockTarget.getPosition()).thenReturn(new Coords(10, 18));
        TestCase.assertEquals(mockAmmoAtm5St, testFireControl.getPreferredAmmo(mockShooter, mockTarget, mockAtm5));
        Mockito.when(mockTarget.getPosition()).thenReturn(new Coords(10, 16));
        TestCase.assertEquals(mockAmmoAtm5He, testFireControl.getPreferredAmmo(mockShooter, mockTarget, mockAtm5));
        Mockito.when(mockTarget.getPosition()).thenReturn(new Coords(10, 15));
        TestCase.assertEquals(mockAmmoAtm5St, testFireControl.getPreferredAmmo(mockShooter, mockTarget, mockAtm5));
        Mockito.when(mockTarget.getPosition()).thenReturn(new Coords(10, 13));
        TestCase.assertEquals(mockAmmoAtm5He, testFireControl.getPreferredAmmo(mockShooter, mockTarget, mockAtm5));

        // Test shooting an AC5 at a building.
        mockTarget = Mockito.mock(BuildingTarget.class);
        Mockito.when(mockTarget.getPosition()).thenReturn(new Coords(10, 15));
        TestCase.assertEquals(mockAmmoAc5Incendiary, testFireControl.getPreferredAmmo(mockShooter, mockTarget,
                                                                                      mockWeaponTypeAC5));

        // Test shooting an LBX at an airborne target.
        mockTarget = Mockito.mock(VTOL.class);
        Mockito.when(mockTarget.getPosition()).thenReturn(new Coords(10, 15));
        Mockito.when(mockTarget.isAirborne()).thenReturn(true);
        TestCase.assertEquals(mockAmmoLB10XCluster, testFireControl.getPreferredAmmo(mockShooter, mockTarget, mockLB10X));

        // Test shooting an LBX at a tank.
        mockTarget = Mockito.mock(Tank.class);
        Mockito.when(mockTarget.getPosition()).thenReturn(new Coords(10, 15));
        TestCase.assertEquals(mockAmmoLB10XCluster, testFireControl.getPreferredAmmo(mockShooter, mockTarget, mockLB10X));

        // Test shooting an AC at infantry.
        mockTarget = Mockito.mock(Infantry.class);
        Mockito.when(mockTarget.getPosition()).thenReturn(new Coords(10, 15));
        TestCase.assertTrue(
                mockAmmoAc5Flechette.equals(testFireControl.getPreferredAmmo(mockShooter, mockTarget,
                                                                             mockWeaponTypeAC5))
                || mockAmmoAc5Incendiary.equals(testFireControl.getPreferredAmmo(mockShooter, mockTarget,
                                                                                 mockWeaponTypeAC5)));

        // Test a LBX at a heavily damaged target.
        mockTarget = Mockito.mock(BipedMech.class);
        Mockito.when(mockTarget.getPosition()).thenReturn(new Coords(10, 15));
        Mockito.when(((Entity)mockTarget).getDamageLevel()).thenReturn(Entity.DMG_HEAVY);
        TestCase.assertEquals(mockAmmoLB10XCluster, testFireControl.getPreferredAmmo(mockShooter, mockTarget, mockLB10X));

        // Test a hot target.
        Mockito.when(((Entity) mockTarget).getDamageLevel()).thenReturn(Entity.DMG_LIGHT);
        Mockito.when(((Entity)mockTarget).getHeat()).thenReturn(12);
        TestCase.assertEquals(mockAmmoInfero5, testFireControl.getPreferredAmmo(mockShooter, mockTarget,
                                                                                      mockMML5));

        // Test a normal target.
        Mockito.when(((Entity)mockTarget).getHeat()).thenReturn(4);
        TestCase.assertEquals(mockAmmoAC5Std, testFireControl.getPreferredAmmo(mockShooter, mockTarget,
                                                                                mockWeaponTypeAC5));
        TestCase.assertEquals(mockAmmoSRM5, testFireControl.getPreferredAmmo(mockShooter, mockTarget, mockMML5));
    }

    @Test
    public void testCorrectFacing() {
        for (int i = 0; i < 6; i++) {
            int result = FireControl.correctFacing(i);
            TestCase.assertEquals(i, result);
        }

        int testFacing = -10;
        int expected = 2;
        int actual = FireControl.correctFacing(testFacing);
        TestCase.assertEquals(expected, actual);

        testFacing = 10;
        expected = 4;
        actual = FireControl.correctFacing(testFacing);
        TestCase.assertEquals(expected, actual);
    }
}

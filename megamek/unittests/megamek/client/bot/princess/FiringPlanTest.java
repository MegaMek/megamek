/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.bot.princess;

import megamek.common.AmmoType;
import megamek.common.Mounted;
import megamek.common.Targetable;
import megamek.common.WeaponType;
import megamek.common.actions.EntityAction;
import megamek.common.actions.WeaponAttackAction;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.util.Vector;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 3/3/14 3:36 PM
 */
@RunWith(JUnit4.class)
public class FiringPlanTest {

    private static final double TOLERANCE = 0.0001;

    private Targetable mockTarget;
    private WeaponFireInfo mockWeaponFireInfoMG;
    private WeaponAttackAction mockWeaponAttackActionMG;
    private WeaponFireInfo mockWeaponFireInfoPPC;
    private Mounted mockPPC;
    private WeaponAttackAction mockWeaponAttackActionPPC;
    private WeaponFireInfo mockWeaponFireInfoERML;
    private WeaponAttackAction mockWeaponAttackActionERML;

    private FiringPlan testFiringPlan;

    @Before
    public void setUp() {
        mockTarget = Mockito.mock(Targetable.class);

        testFiringPlan = Mockito.spy(new FiringPlan(mockTarget));

        mockWeaponFireInfoMG = Mockito.mock(WeaponFireInfo.class);
        testFiringPlan.add(mockWeaponFireInfoMG);
        Mounted mockMG = Mockito.mock(Mounted.class);
        Mockito.when(mockWeaponFireInfoMG.getWeapon()).thenReturn(mockMG);
        mockWeaponAttackActionMG = Mockito.mock(WeaponAttackAction.class);
        Mockito.when(mockWeaponFireInfoMG.getWeaponAttackAction()).thenReturn(mockWeaponAttackActionMG);

        mockWeaponFireInfoPPC = Mockito.mock(WeaponFireInfo.class);
        testFiringPlan.add(mockWeaponFireInfoPPC);
        mockPPC = Mockito.mock(Mounted.class);
        Mockito.when(mockWeaponFireInfoPPC.getWeapon()).thenReturn(mockPPC);
        mockWeaponAttackActionPPC = Mockito.mock(WeaponAttackAction.class);
        Mockito.when(mockWeaponFireInfoPPC.getWeaponAttackAction()).thenReturn(mockWeaponAttackActionPPC);

        mockWeaponFireInfoERML = Mockito.mock(WeaponFireInfo.class);
        testFiringPlan.add(mockWeaponFireInfoERML);
        Mounted mockERML = Mockito.mock(Mounted.class);
        Mockito.when(mockWeaponFireInfoERML.getWeapon()).thenReturn(mockERML);
        mockWeaponAttackActionERML = Mockito.mock(WeaponAttackAction.class);
        Mockito.when(mockWeaponFireInfoERML.getWeaponAttackAction()).thenReturn(mockWeaponAttackActionERML);
    }

    @Test
    public void testGetHeat() {
        Mockito.when(mockWeaponFireInfoMG.getHeat()).thenReturn(0);
        Mockito.when(mockWeaponFireInfoPPC.getHeat()).thenReturn(10);
        Mockito.when(mockWeaponFireInfoERML.getHeat()).thenReturn(5);

        int expected = 15;
        Assert.assertEquals(expected, testFiringPlan.getHeat());
    }

    @Test
    public void testGetExpectedDamage() {
        Mockito.when(mockWeaponFireInfoMG.getExpectedDamageOnHit()).thenReturn(2.0);
        Mockito.when(mockWeaponFireInfoMG.getProbabilityToHit()).thenReturn(0.1666);
        Mockito.when(mockWeaponFireInfoPPC.getExpectedDamageOnHit()).thenReturn(10.0);
        Mockito.when(mockWeaponFireInfoPPC.getProbabilityToHit()).thenReturn(0.7222);
        Mockito.when(mockWeaponFireInfoERML.getExpectedDamageOnHit()).thenReturn(5.0);
        Mockito.when(mockWeaponFireInfoERML.getProbabilityToHit()).thenReturn(0.4166);

        double expected = (2 * 0.1666) + (10 * 0.7222) + (5 * 0.4166);
        Assert.assertEquals(expected, testFiringPlan.getExpectedDamage(), TOLERANCE);
    }

    @Test
    public void testGetExpectedCriticals() {
        Mockito.when(mockWeaponFireInfoMG.getExpectedCriticals()).thenReturn(0.0);
        Mockito.when(mockWeaponFireInfoPPC.getExpectedCriticals()).thenReturn(0.423);
        Mockito.when(mockWeaponFireInfoERML.getExpectedCriticals()).thenReturn(0.015);

        double expected = 0 + 0.423 + 0.015;
        Assert.assertEquals(expected, testFiringPlan.getExpectedCriticals(), TOLERANCE);
    }

    @Test
    public void testGetKillProbability() {
        Mockito.when(mockWeaponFireInfoMG.getKillProbability()).thenReturn(0.0);
        Mockito.when(mockWeaponFireInfoPPC.getKillProbability()).thenReturn(0.0024);
        Mockito.when(mockWeaponFireInfoERML.getKillProbability()).thenReturn(0.0);

        //noinspection PointlessArithmeticExpression
        double expected = 1 - ((1 - 0) * (1 - 0.0024) * (1 - 0));
        Assert.assertEquals(expected, testFiringPlan.getKillProbability(), TOLERANCE);
        
        Mockito.when(mockWeaponFireInfoMG.getKillProbability()).thenReturn(1.0);
        Mockito.when(mockWeaponFireInfoPPC.getKillProbability()).thenReturn(0.0024);
        Mockito.when(mockWeaponFireInfoERML.getKillProbability()).thenReturn(0.0);

        //noinspection PointlessArithmeticExpression
        expected = 1 - ((1 - 1) * (1 - 0.0024) * (1 - 0));
        Assert.assertEquals(expected, testFiringPlan.getKillProbability(), TOLERANCE);
        
        Mockito.when(mockWeaponFireInfoMG.getKillProbability()).thenReturn(0.5);
        Mockito.when(mockWeaponFireInfoPPC.getKillProbability()).thenReturn(0.5);
        Mockito.when(mockWeaponFireInfoERML.getKillProbability()).thenReturn(0.5);

        expected = 1 - ((1 - 0.5) * (1 - 0.5) * (1 - 0.5));
        Assert.assertEquals(expected, testFiringPlan.getKillProbability(), TOLERANCE);
    }

    @Test
    public void testContainsWeapon() {

        // Test a weapon that is in the plan.
        Assert.assertTrue(testFiringPlan.containsWeapon(mockPPC));

        // Test a weapon that is not in the plan.
        Assert.assertFalse(testFiringPlan.containsWeapon(Mockito.mock(Mounted.class)));
    }

    @Test
    public void testGetEntityActionVector() {
        Vector<EntityAction> expected;

        // Test a no-twist plan.
        Mockito.when(testFiringPlan.getTwist()).thenReturn(0);
        expected = new Vector<>(3);
        expected.add(mockWeaponAttackActionMG);
        expected.add(mockWeaponAttackActionPPC);
        expected.add(mockWeaponAttackActionERML);
        Assert.assertEquals(expected, testFiringPlan.getEntityActionVector());

        // todo Test torso-twists.

        // Test an empty firing plan.
        //noinspection MismatchedQueryAndUpdateOfCollection
        FiringPlan emptyPlan = new FiringPlan(mockTarget);
        expected = new Vector<>(0);
        Assert.assertEquals(expected, emptyPlan.getEntityActionVector());
    }

    @Test
    public void testSortPlan() {
        WeaponFireInfo mockInfoLL = Mockito.mock(WeaponFireInfo.class);
        Mounted mockLL = Mockito.mock(Mounted.class);
        Mockito.when(mockLL.getName()).thenReturn("LL");
        Mockito.when(mockLL.toString()).thenReturn("LL");
        Mockito.when(mockInfoLL.getWeapon()).thenReturn(mockLL);
        WeaponType mockTypeLL = Mockito.mock(WeaponType.class);
        Mockito.when(mockLL.getType()).thenReturn(mockTypeLL);
        Mockito.when(mockTypeLL.getDamage()).thenReturn(8);
        Mockito.when(mockLL.getLinked()).thenReturn(null);

        WeaponFireInfo mockInfoLRM = Mockito.mock(WeaponFireInfo.class);
        Mounted mockLRM = Mockito.mock(Mounted.class);
        Mockito.when(mockLRM.getName()).thenReturn("LRM");
        Mockito.when(mockLRM.toString()).thenReturn("LRM");
        Mockito.when(mockInfoLRM.getWeapon()).thenReturn(mockLRM);
        WeaponType mockTypeLRM = Mockito.mock(WeaponType.class);
        Mockito.when(mockLRM.getType()).thenReturn(mockTypeLRM);
        Mockito.when(mockTypeLRM.getDamage()).thenReturn(WeaponType.DAMAGE_BY_CLUSTERTABLE);
        Mounted mockAmmoLRM = Mockito.mock(Mounted.class);
        Mockito.when(mockLRM.getLinked()).thenReturn(mockAmmoLRM);
        AmmoType mockAmmoTypeLRM = Mockito.mock(AmmoType.class);
        Mockito.when(mockAmmoLRM.getType()).thenReturn(mockAmmoTypeLRM);
        Mockito.when(mockAmmoTypeLRM.getMunitionType()).thenReturn(AmmoType.M_STANDARD);
        Mockito.when(mockAmmoTypeLRM.getDamagePerShot()).thenReturn(1);

        WeaponFireInfo mockInfoLBX = Mockito.mock(WeaponFireInfo.class);
        Mounted mockLBX = Mockito.mock(Mounted.class);
        Mockito.when(mockLBX.getName()).thenReturn("LBX");
        Mockito.when(mockLBX.toString()).thenReturn("LBX");
        Mockito.when(mockInfoLBX.getWeapon()).thenReturn(mockLBX);
        WeaponType mockTypeLBX = Mockito.mock(WeaponType.class);
        Mockito.when(mockLBX.getType()).thenReturn(mockTypeLBX);
        Mockito.when(mockTypeLBX.getDamage()).thenReturn(10);
        Mounted mockAmmoLBX = Mockito.mock(Mounted.class);
        Mockito.when(mockLBX.getLinked()).thenReturn(mockAmmoLBX);
        AmmoType mockAmmoTypeLBXCluster = Mockito.mock(AmmoType.class);
        Mockito.when(mockAmmoTypeLBXCluster.getMunitionType()).thenReturn(AmmoType.M_CLUSTER);
        Mockito.when(mockAmmoTypeLBXCluster.getDamagePerShot()).thenReturn(1);
        AmmoType mockAmmoTypeLBXSlug = Mockito.mock(AmmoType.class);
        Mockito.when(mockAmmoTypeLBXSlug.getMunitionType()).thenReturn(AmmoType.M_STANDARD);
        Mockito.when(mockAmmoTypeLBXSlug.getDamagePerShot()).thenReturn(1);

        WeaponFireInfo mockInfoSRM = Mockito.mock(WeaponFireInfo.class);
        Mounted mockSRM = Mockito.mock(Mounted.class);
        Mockito.when(mockSRM.getName()).thenReturn("SRM");
        Mockito.when(mockSRM.toString()).thenReturn("SRM");
        Mockito.when(mockInfoSRM.getWeapon()).thenReturn(mockSRM);
        WeaponType mockTypeSRM = Mockito.mock(WeaponType.class);
        Mockito.when(mockSRM.getType()).thenReturn(mockTypeSRM);
        Mockito.when(mockTypeSRM.getDamage()).thenReturn(WeaponType.DAMAGE_BY_CLUSTERTABLE);
        Mounted mockAmmoSRM = Mockito.mock(Mounted.class);
        Mockito.when(mockSRM.getLinked()).thenReturn(mockAmmoSRM);
        AmmoType mockAmmoTypeSRM = Mockito.mock(AmmoType.class);
        Mockito.when(mockAmmoSRM.getType()).thenReturn(mockAmmoTypeSRM);
        Mockito.when(mockAmmoTypeSRM.getMunitionType()).thenReturn(AmmoType.M_STANDARD);
        Mockito.when(mockAmmoTypeSRM.getDamagePerShot()).thenReturn(2);

        FiringPlan testPlan = Mockito.spy(new FiringPlan(mockTarget));

        // Test the plan with the LBX firing cluster ammo.
        Mockito.when(mockAmmoLBX.getType()).thenReturn(mockAmmoTypeLBXCluster);
        testPlan.add(mockInfoLBX);
        testPlan.add(mockInfoLRM);
        testPlan.add(mockInfoLL);
        testPlan.add(mockInfoSRM);
        FiringPlan expectedOrder = new FiringPlan(mockTarget);
        expectedOrder.add(mockInfoLL);
        expectedOrder.add(mockInfoSRM);
        expectedOrder.add(mockInfoLBX);
        expectedOrder.add(mockInfoLRM);
        testPlan.sortPlan();
        Assert.assertEquals("\nExpected: " + expectedOrder.getWeaponNames() + "\nActual  : " + testPlan
                                    .getWeaponNames(),
                            expectedOrder, testPlan);

        // Test the plan with LBX firing slugs.
        Mockito.when(mockAmmoLBX.getType()).thenReturn(mockAmmoTypeLBXSlug);
        testPlan.clear();
        testPlan.add(mockInfoLBX);
        testPlan.add(mockInfoLRM);
        testPlan.add(mockInfoLL);
        testPlan.add(mockInfoSRM);
        expectedOrder = new FiringPlan(mockTarget);
        expectedOrder.add(mockInfoLBX);
        expectedOrder.add(mockInfoLL);
        expectedOrder.add(mockInfoSRM);
        expectedOrder.add(mockInfoLRM);
        testPlan.sortPlan();
        Assert.assertEquals("\nExpected: " + expectedOrder.getWeaponNames() + "\nActual  : " + testPlan
                                    .getWeaponNames(),
                            expectedOrder, testPlan);
    }
}

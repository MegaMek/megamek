/*
 * Copyright (c) 2000-2011 - Ben Mazur (bmazur@sev.org)
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
package megamek.client.bot.princess;

import megamek.common.AmmoType;
import megamek.common.Mounted;
import megamek.common.Targetable;
import megamek.common.WeaponType;
import megamek.common.actions.EntityAction;
import megamek.common.actions.WeaponAttackAction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 3/3/14 3:36 PM
 */
public class FiringPlanTest {

    private static final double TOLERANCE = 0.0001;

    private static Targetable mockTarget;
    private static WeaponFireInfo mockWeaponFireInfoMG;
    private static WeaponAttackAction mockWeaponAttackActionMG;
    private static WeaponFireInfo mockWeaponFireInfoPPC;
    private static Mounted mockPPC;
    private static WeaponAttackAction mockWeaponAttackActionPPC;
    private static WeaponFireInfo mockWeaponFireInfoERML;
    private static WeaponAttackAction mockWeaponAttackActionERML;

    private static FiringPlan testFiringPlan;

    @BeforeAll
    public static void beforeAll() {
        mockTarget = mock(Targetable.class);

        testFiringPlan = spy(new FiringPlan(mockTarget));

        mockWeaponFireInfoMG = mock(WeaponFireInfo.class);
        testFiringPlan.add(mockWeaponFireInfoMG);
        Mounted mockMG = mock(Mounted.class);
        when(mockWeaponFireInfoMG.getWeapon()).thenReturn(mockMG);
        mockWeaponAttackActionMG = mock(WeaponAttackAction.class);
        when(mockWeaponFireInfoMG.getWeaponAttackAction()).thenReturn(mockWeaponAttackActionMG);

        mockWeaponFireInfoPPC = mock(WeaponFireInfo.class);
        testFiringPlan.add(mockWeaponFireInfoPPC);
        mockPPC = mock(Mounted.class);
        when(mockWeaponFireInfoPPC.getWeapon()).thenReturn(mockPPC);
        mockWeaponAttackActionPPC = mock(WeaponAttackAction.class);
        when(mockWeaponFireInfoPPC.getWeaponAttackAction()).thenReturn(mockWeaponAttackActionPPC);

        mockWeaponFireInfoERML = mock(WeaponFireInfo.class);
        testFiringPlan.add(mockWeaponFireInfoERML);
        Mounted mockERML = mock(Mounted.class);
        when(mockWeaponFireInfoERML.getWeapon()).thenReturn(mockERML);
        mockWeaponAttackActionERML = mock(WeaponAttackAction.class);
        when(mockWeaponFireInfoERML.getWeaponAttackAction()).thenReturn(mockWeaponAttackActionERML);
    }

    @Test
    public void testGetHeat() {
        when(mockWeaponFireInfoMG.getHeat()).thenReturn(0);
        when(mockWeaponFireInfoPPC.getHeat()).thenReturn(10);
        when(mockWeaponFireInfoERML.getHeat()).thenReturn(5);

        int expected = 15;
        assertEquals(expected, testFiringPlan.getHeat());
    }

    @Test
    public void testGetExpectedDamage() {
        when(mockWeaponFireInfoMG.getExpectedDamageOnHit()).thenReturn(2.0);
        when(mockWeaponFireInfoMG.getProbabilityToHit()).thenReturn(0.1666);
        when(mockWeaponFireInfoPPC.getExpectedDamageOnHit()).thenReturn(10.0);
        when(mockWeaponFireInfoPPC.getProbabilityToHit()).thenReturn(0.7222);
        when(mockWeaponFireInfoERML.getExpectedDamageOnHit()).thenReturn(5.0);
        when(mockWeaponFireInfoERML.getProbabilityToHit()).thenReturn(0.4166);

        double expected = (2 * 0.1666) + (10 * 0.7222) + (5 * 0.4166);
        assertEquals(expected, testFiringPlan.getExpectedDamage(), TOLERANCE);
    }

    @Test
    public void testGetExpectedCriticals() {
        when(mockWeaponFireInfoMG.getExpectedCriticals()).thenReturn(0.0);
        when(mockWeaponFireInfoPPC.getExpectedCriticals()).thenReturn(0.423);
        when(mockWeaponFireInfoERML.getExpectedCriticals()).thenReturn(0.015);

        double expected = 0 + 0.423 + 0.015;
        assertEquals(expected, testFiringPlan.getExpectedCriticals(), TOLERANCE);
    }

    @Test
    public void testGetKillProbability() {
        when(mockWeaponFireInfoMG.getKillProbability()).thenReturn(0.0);
        when(mockWeaponFireInfoPPC.getKillProbability()).thenReturn(0.0024);
        when(mockWeaponFireInfoERML.getKillProbability()).thenReturn(0.0);

        //noinspection PointlessArithmeticExpression
        double expected = 1 - ((1 - 0) * (1 - 0.0024) * (1 - 0));
        assertEquals(expected, testFiringPlan.getKillProbability(), TOLERANCE);

        when(mockWeaponFireInfoMG.getKillProbability()).thenReturn(1.0);
        when(mockWeaponFireInfoPPC.getKillProbability()).thenReturn(0.0024);
        when(mockWeaponFireInfoERML.getKillProbability()).thenReturn(0.0);

        //noinspection PointlessArithmeticExpression
        expected = 1 - ((1 - 1) * (1 - 0.0024) * (1 - 0));
        assertEquals(expected, testFiringPlan.getKillProbability(), TOLERANCE);

        when(mockWeaponFireInfoMG.getKillProbability()).thenReturn(0.5);
        when(mockWeaponFireInfoPPC.getKillProbability()).thenReturn(0.5);
        when(mockWeaponFireInfoERML.getKillProbability()).thenReturn(0.5);

        expected = 1 - ((1 - 0.5) * (1 - 0.5) * (1 - 0.5));
        assertEquals(expected, testFiringPlan.getKillProbability(), TOLERANCE);
    }

    @Test
    public void testContainsWeapon() {
        // Test a weapon that is in the plan.
        assertTrue(testFiringPlan.containsWeapon(mockPPC));

        // Test a weapon that is not in the plan.
        assertFalse(testFiringPlan.containsWeapon(mock(Mounted.class)));
    }

    @Test
    public void testGetEntityActionVector() {
        // Test a no-twist plan.
        when(testFiringPlan.getTwist()).thenReturn(0);
        Vector<EntityAction> expected = new Vector<>(3);
        expected.add(mockWeaponAttackActionMG);
        expected.add(mockWeaponAttackActionPPC);
        expected.add(mockWeaponAttackActionERML);
        assertEquals(expected, testFiringPlan.getEntityActionVector());

        // todo Test torso-twists.

        // Test an empty firing plan.
        //noinspection MismatchedQueryAndUpdateOfCollection
        FiringPlan emptyPlan = new FiringPlan(mockTarget);
        expected = new Vector<>(0);
        assertEquals(expected, emptyPlan.getEntityActionVector());
    }

    @Test
    public void testSortPlan() {
        WeaponFireInfo mockInfoLL = mock(WeaponFireInfo.class);
        Mounted mockLL = mock(Mounted.class);
        when(mockLL.getName()).thenReturn("LL");
        when(mockLL.toString()).thenReturn("LL");
        when(mockInfoLL.getWeapon()).thenReturn(mockLL);
        WeaponType mockTypeLL = mock(WeaponType.class);
        when(mockLL.getType()).thenReturn(mockTypeLL);
        when(mockTypeLL.getDamage()).thenReturn(8);
        when(mockLL.getLinked()).thenReturn(null);

        WeaponFireInfo mockInfoLRM = mock(WeaponFireInfo.class);
        Mounted mockLRM = mock(Mounted.class);
        when(mockLRM.getName()).thenReturn("LRM");
        when(mockLRM.toString()).thenReturn("LRM");
        when(mockInfoLRM.getWeapon()).thenReturn(mockLRM);
        WeaponType mockTypeLRM = mock(WeaponType.class);
        when(mockLRM.getType()).thenReturn(mockTypeLRM);
        when(mockTypeLRM.getDamage()).thenReturn(WeaponType.DAMAGE_BY_CLUSTERTABLE);
        Mounted mockAmmoLRM = mock(Mounted.class);
        when(mockLRM.getLinked()).thenReturn(mockAmmoLRM);
        AmmoType mockAmmoTypeLRM = mock(AmmoType.class);
        when(mockAmmoLRM.getType()).thenReturn(mockAmmoTypeLRM);
        when(mockAmmoTypeLRM.getMunitionType()).thenReturn(EnumSet.of(AmmoType.Munitions.M_STANDARD));
        when(mockAmmoTypeLRM.getDamagePerShot()).thenReturn(1);

        WeaponFireInfo mockInfoLBX = mock(WeaponFireInfo.class);
        Mounted mockLBX = mock(Mounted.class);
        when(mockLBX.getName()).thenReturn("LBX");
        when(mockLBX.toString()).thenReturn("LBX");
        when(mockInfoLBX.getWeapon()).thenReturn(mockLBX);
        WeaponType mockTypeLBX = mock(WeaponType.class);
        when(mockLBX.getType()).thenReturn(mockTypeLBX);
        when(mockTypeLBX.getDamage()).thenReturn(10);
        Mounted mockAmmoLBX = mock(Mounted.class);
        when(mockLBX.getLinked()).thenReturn(mockAmmoLBX);
        AmmoType mockAmmoTypeLBXCluster = mock(AmmoType.class);
        when(mockAmmoTypeLBXCluster.getMunitionType()).thenReturn(EnumSet.of(AmmoType.Munitions.M_CLUSTER));
        when(mockAmmoTypeLBXCluster.getDamagePerShot()).thenReturn(1);
        AmmoType mockAmmoTypeLBXSlug = mock(AmmoType.class);
        when(mockAmmoTypeLBXSlug.getMunitionType()).thenReturn(EnumSet.of(AmmoType.Munitions.M_STANDARD));
        when(mockAmmoTypeLBXSlug.getDamagePerShot()).thenReturn(1);

        WeaponFireInfo mockInfoSRM = mock(WeaponFireInfo.class);
        Mounted mockSRM = mock(Mounted.class);
        when(mockSRM.getName()).thenReturn("SRM");
        when(mockSRM.toString()).thenReturn("SRM");
        when(mockInfoSRM.getWeapon()).thenReturn(mockSRM);
        WeaponType mockTypeSRM = mock(WeaponType.class);
        when(mockSRM.getType()).thenReturn(mockTypeSRM);
        when(mockTypeSRM.getDamage()).thenReturn(WeaponType.DAMAGE_BY_CLUSTERTABLE);
        Mounted mockAmmoSRM = mock(Mounted.class);
        when(mockSRM.getLinked()).thenReturn(mockAmmoSRM);
        AmmoType mockAmmoTypeSRM = mock(AmmoType.class);
        when(mockAmmoSRM.getType()).thenReturn(mockAmmoTypeSRM);
        when(mockAmmoTypeSRM.getMunitionType()).thenReturn(EnumSet.of(AmmoType.Munitions.M_STANDARD));
        when(mockAmmoTypeSRM.getDamagePerShot()).thenReturn(2);

        FiringPlan testPlan = spy(new FiringPlan(mockTarget));

        // Test the plan with the LBX firing cluster ammo.
        when(mockAmmoLBX.getType()).thenReturn(mockAmmoTypeLBXCluster);
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
        assertEquals(expectedOrder, testPlan,
                "\nExpected: " + expectedOrder.getWeaponNames() + "\nActual  : " + testPlan.getWeaponNames());

        // Test the plan with LBX firing slugs.
        when(mockAmmoLBX.getType()).thenReturn(mockAmmoTypeLBXSlug);
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
        assertEquals(expectedOrder, testPlan,
                "\nExpected: " + expectedOrder.getWeaponNames() + "\nActual  : " + testPlan.getWeaponNames());
    }
}

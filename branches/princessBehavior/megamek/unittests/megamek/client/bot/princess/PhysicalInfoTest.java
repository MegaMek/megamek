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
import megamek.common.BipedMech;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.IGame;
import megamek.common.ToHitData;
import megamek.common.util.LogLevel;
import megamek.common.util.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

/**
 * PhysicalInfo is a wrapper around a PhysicalAttackAction that includes
 * probability to hit and expected damage
 *
 * @version %Id%
 * @author: Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since: 9/12/13 8:16 PM
 */
@RunWith(JUnit4.class)
public class PhysicalInfoTest {

    private final Coords SHOOTER_COORDS = new Coords(10, 10);

    private final Coords TARGET_ADJACENT_COORDS = new Coords(10,11);
    private final Coords TARGET_NON_ADJACENT_COORDS = new Coords(10,15);
    private final int TARGET_FACING = 3;

    ToHitData mockToHitEight = Mockito.mock(ToHitData.class);
    ToHitData mockToHitNonAdjacent = Mockito.mock(ToHitData.class);
    ToHitData mockToHitSix = Mockito.mock(ToHitData.class);
    BipedMech mockShooter      = Mockito.mock(BipedMech.class);
    EntityState mockShooterState = Mockito.mock(EntityState.class);
    BipedMech  mockTarget       = Mockito.mock(BipedMech.class);
    EntityState mockTargetState  = Mockito.mock(EntityState.class);
    IGame       mockGame         = Mockito.mock(IGame.class);

    @Before
    public void setUp() {
        Logger.setVerbosity(LogLevel.DEBUG);

        Mockito.when(mockToHitSix.getValue()).thenReturn(6);
        Mockito.when(mockToHitEight.getValue()).thenReturn(8);
        Mockito.when(mockToHitNonAdjacent.getValue()).thenReturn(ToHitData.AUTOMATIC_FAIL);

        Mockito.when(mockShooter.getPosition()).thenReturn(SHOOTER_COORDS);
        Mockito.when(mockShooter.getWeight()).thenReturn(75.0f);
        Mockito.when(mockShooterState.getPosition()).thenReturn(SHOOTER_COORDS);

        Mockito.when(mockTarget.getPosition()).thenReturn(TARGET_ADJACENT_COORDS);
        Mockito.when(mockTarget.isLocationBad(Mockito.anyInt())).thenReturn(false);
        Mockito.when(mockTargetState.getFacing()).thenReturn(TARGET_FACING);
        Mockito.when(mockTargetState.getPosition()).thenReturn(TARGET_ADJACENT_COORDS);
    }

    @Test
    public void testInitDamage() {
        final double DELTA = 0.000001;

        PhysicalInfo testPhysicalInfo = Mockito.spy(new PhysicalInfo());
        testPhysicalInfo.setShooter(mockShooter);
        testPhysicalInfo.setShooterState(mockShooterState);
        testPhysicalInfo.setTarget(mockTarget);
        testPhysicalInfo.setTargetState(mockTargetState);
        testPhysicalInfo.setGame(mockGame);

        // Test a punch against an adjacent mech target with armor/internals of 10/6 on all locations except a head of
        // 9/3.
        Mockito.when(mockTarget.getArmor(Mockito.anyInt(), Mockito.anyBoolean())).thenReturn(10);
        Mockito.when(mockTarget.getArmor(Mockito.eq(0), Mockito.anyBoolean())).thenReturn(9);
        Mockito.when(mockTarget.getInternal(Mockito.anyInt())).thenReturn(6);
        Mockito.when(mockTarget.getInternal(Mockito.eq(0))).thenReturn(3);
        testPhysicalInfo.setAttackType(PhysicalAttackType.RIGHT_PUNCH);
        double expectedMaxDamage = Math.ceil(mockShooter.getWeight() / 10.0);
        double expectedProbabilityToHit = Compute.oddsAbove(mockToHitEight.getValue());
        double expectedCriticals = 0.0004756024;
        double expectedKill = 0;
        double expectedUtility = 339.916928;
        Mockito.doReturn(mockToHitEight).when(testPhysicalInfo).getToHit();
        testPhysicalInfo.initDamage();
        TestCase.assertEquals(expectedMaxDamage, testPhysicalInfo.getMaxDamage());
        TestCase.assertEquals(expectedMaxDamage, testPhysicalInfo.getExpectedDamageOnHit());
        TestCase.assertEquals(expectedProbabilityToHit, testPhysicalInfo.getProbabilityToHit());
        TestCase.assertEquals(expectedCriticals, testPhysicalInfo.getExpectedCriticals(), DELTA);
        TestCase.assertEquals(expectedKill, testPhysicalInfo.getKillProbability(), DELTA);
        TestCase.assertEquals(expectedUtility, testPhysicalInfo.getUtility(), DELTA);

        // Test a punch against an adjacent mech target with armor/internals of 6/6 on all locations except a head of
        // 4/3.
        Mockito.when(mockTarget.getArmor(Mockito.anyInt(), Mockito.anyBoolean())).thenReturn(6);
        Mockito.when(mockTarget.getArmor(Mockito.eq(0), Mockito.anyBoolean())).thenReturn(4);
        expectedCriticals = 28.8263594667;
        expectedKill = 6.9333333333;
        expectedUtility = 967.7302613333;
        testPhysicalInfo.initDamage();
        TestCase.assertEquals(expectedMaxDamage, testPhysicalInfo.getMaxDamage());
        TestCase.assertEquals(expectedMaxDamage, testPhysicalInfo.getExpectedDamageOnHit());
        TestCase.assertEquals(expectedProbabilityToHit, testPhysicalInfo.getProbabilityToHit());
        TestCase.assertEquals(expectedCriticals, testPhysicalInfo.getExpectedCriticals(), DELTA);
        TestCase.assertEquals(expectedKill, testPhysicalInfo.getKillProbability(), DELTA);
        TestCase.assertEquals(expectedUtility, testPhysicalInfo.getUtility(), DELTA);

        // Test a punch against a non-adjacent mech.
        Mockito.when(mockTarget.getPosition()).thenReturn(TARGET_NON_ADJACENT_COORDS);
        Mockito.when(mockTargetState.getPosition()).thenReturn(TARGET_NON_ADJACENT_COORDS);
        Mockito.doReturn(mockToHitNonAdjacent).when(testPhysicalInfo).getToHit();
        expectedCriticals = 0;
        expectedKill = 0;
        expectedProbabilityToHit = Compute.oddsAbove(mockToHitNonAdjacent.getValue());
        expectedUtility = 0;
        testPhysicalInfo.initDamage();
        TestCase.assertEquals(expectedMaxDamage, testPhysicalInfo.getMaxDamage());
        TestCase.assertEquals(expectedMaxDamage, testPhysicalInfo.getExpectedDamageOnHit());
        TestCase.assertEquals(expectedProbabilityToHit, testPhysicalInfo.getProbabilityToHit());
        TestCase.assertEquals(expectedCriticals, testPhysicalInfo.getExpectedCriticals(), DELTA);
        TestCase.assertEquals(expectedKill, testPhysicalInfo.getKillProbability(), DELTA);
        TestCase.assertEquals(expectedUtility, testPhysicalInfo.getUtility(), DELTA);

        // Test a kick against an adjacent mech target with armor/internals of 10/6 on all locations except a head of
        // 9/3.
        Mockito.when(mockTarget.getArmor(Mockito.anyInt(), Mockito.anyBoolean())).thenReturn(10);
        Mockito.when(mockTarget.getArmor(Mockito.eq(0), Mockito.anyBoolean())).thenReturn(9);
        Mockito.doReturn(mockToHitEight).when(testPhysicalInfo).getToHit();
        testPhysicalInfo.setAttackType(PhysicalAttackType.RIGHT_KICK);
        expectedMaxDamage = Math.ceil(mockShooter.getWeight() / 5.0);
        expectedCriticals = 26.1292928;
        expectedKill = 0;
        expectedUtility = 885.292928;
        expectedProbabilityToHit = Compute.oddsAbove(mockToHitEight.getValue());
        testPhysicalInfo.initDamage();
        TestCase.assertEquals(expectedMaxDamage, testPhysicalInfo.getMaxDamage());
        TestCase.assertEquals(expectedMaxDamage, testPhysicalInfo.getExpectedDamageOnHit());
        TestCase.assertEquals(expectedProbabilityToHit, testPhysicalInfo.getProbabilityToHit());
        TestCase.assertEquals(expectedCriticals, testPhysicalInfo.getExpectedCriticals(), DELTA);
        TestCase.assertEquals(expectedKill, testPhysicalInfo.getKillProbability(), DELTA);
        TestCase.assertEquals(expectedUtility, testPhysicalInfo.getUtility(), DELTA);

        // Improve the to-hit roll of the kick to 6.
        Mockito.doReturn(mockToHitSix).when(testPhysicalInfo).getToHit();
        expectedCriticals = 45.3493976;
        expectedUtility = 1536.493976;
        expectedProbabilityToHit = Compute.oddsAbove(mockToHitSix.getValue());
        testPhysicalInfo.initDamage();
        TestCase.assertEquals(expectedMaxDamage, testPhysicalInfo.getMaxDamage());
        TestCase.assertEquals(expectedMaxDamage, testPhysicalInfo.getExpectedDamageOnHit());
        TestCase.assertEquals(expectedProbabilityToHit, testPhysicalInfo.getProbabilityToHit());
        TestCase.assertEquals(expectedCriticals, testPhysicalInfo.getExpectedCriticals(), DELTA);
        TestCase.assertEquals(expectedKill, testPhysicalInfo.getKillProbability(), DELTA);
        TestCase.assertEquals(expectedUtility, testPhysicalInfo.getUtility(), DELTA);
    }
}

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
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.Mounted;
import megamek.common.Targetable;
import megamek.common.actions.EntityAction;
import megamek.common.actions.TorsoTwistAction;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * @version %Id%
 * @lastEditBy Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since: 9/17/13 2:38 PM
 */
@RunWith(JUnit4.class)
public class FiringPlanTest {
    private final int TO_HIT = 7;
    private final double TO_HIT_CHANCE = Compute.oddsAbove(TO_HIT) / 100;

    private Targetable mockTarget = Mockito.mock(Targetable.class);
    private Entity mockShooter = Mockito.mock(Entity.class);
    private WeaponFireInfo mockMediumLaserInfo = Mockito.mock(WeaponFireInfo.class);
    private Mounted mockMediumLaser = Mockito.mock(Mounted.class);
    private WeaponFireInfo mockPPCInfo = Mockito.mock(WeaponFireInfo.class);
    private Mounted mockPPC = Mockito.mock(Mounted.class);
    private WeaponFireInfo mockLRM20Info = Mockito.mock(WeaponFireInfo.class);
    private Mounted mockLRM20 = Mockito.mock(Mounted.class);

    @Before
    public void setUp() {
        Mockito.when(mockShooter.getId()).thenReturn(1);
        Mockito.when(mockShooter.getFacing()).thenReturn(3);
        Mockito.when(mockShooter.getDisplayName()).thenReturn("Shooter #1");

        Mockito.when(mockTarget.getDisplayName()).thenReturn("Target #2");

        Mockito.when(mockMediumLaser.getName()).thenReturn("Medium Laser");
        Mockito.when(mockMediumLaserInfo.getHeat()).thenReturn(3);
        double maxDmg = 5.0;
        double dmg = TO_HIT_CHANCE * 5;
        Mockito.when(mockMediumLaserInfo.getExpectedDamage()).thenReturn(dmg);
        Mockito.when(mockMediumLaserInfo.getExpectedCriticals()).thenReturn(0.0);
        Mockito.when(mockMediumLaserInfo.getKillProbability()).thenReturn(0.0);
        Mockito.when(mockMediumLaserInfo.getWeapon()).thenReturn(mockMediumLaser);
        Mockito.when(mockMediumLaserInfo.getShooter()).thenReturn(mockShooter);
        Mockito.when(mockMediumLaserInfo.getProbabilityToHit()).thenReturn(TO_HIT_CHANCE);
        Mockito.when(mockMediumLaserInfo.getMaxDamage()).thenReturn(maxDmg);
        Mockito.when(mockMediumLaserInfo.getExpectedDamageOnHit()).thenReturn(maxDmg);
        Mockito.when(mockMediumLaserInfo.getDebugDescription()).thenCallRealMethod();

        Mockito.when(mockPPC.getName()).thenReturn("PPC");
        Mockito.when(mockPPCInfo.getHeat()).thenReturn(10);
        maxDmg = 10;
        dmg = TO_HIT_CHANCE * 10;
        Mockito.when(mockPPCInfo.getExpectedDamage()).thenReturn(dmg);
        Mockito.when(mockPPCInfo.getExpectedCriticals()).thenReturn(0.02460);
        Mockito.when(mockPPCInfo.getKillProbability()).thenReturn(0.01155);
        Mockito.when(mockPPCInfo.getWeapon()).thenReturn(mockPPC);
        Mockito.when(mockPPCInfo.getShooter()).thenReturn(mockShooter);
        Mockito.when(mockPPCInfo.getProbabilityToHit()).thenReturn(TO_HIT_CHANCE);
        Mockito.when(mockPPCInfo.getMaxDamage()).thenReturn(maxDmg);
        Mockito.when(mockPPCInfo.getExpectedDamageOnHit()).thenReturn(maxDmg);
        Mockito.when(mockPPCInfo.getDebugDescription()).thenCallRealMethod();

        Mockito.when(mockLRM20.getName()).thenReturn("LRM20");
        Mockito.when(mockLRM20Info.getHeat()).thenReturn(7);
        maxDmg = Compute.calculateClusterHitTableAmount(7, 20);
        dmg = TO_HIT_CHANCE * maxDmg;
        Mockito.when(mockLRM20Info.getExpectedDamage()).thenReturn(dmg);
        Mockito.when(mockLRM20Info.getExpectedCriticals()).thenReturn(0.01867);
        Mockito.when(mockLRM20Info.getKillProbability()).thenReturn(0.00825);
        Mockito.when(mockLRM20Info.getWeapon()).thenReturn(mockLRM20);
        Mockito.when(mockLRM20Info.getShooter()).thenReturn(mockShooter);
        Mockito.when(mockLRM20Info.getProbabilityToHit()).thenReturn(TO_HIT_CHANCE);
        Mockito.when(mockLRM20Info.getMaxDamage()).thenReturn(maxDmg);
        Mockito.when(mockLRM20Info.getExpectedDamageOnHit()).thenReturn(maxDmg);
        Mockito.when(mockLRM20Info.getDebugDescription()).thenCallRealMethod();
    }

    @Test
    public void testGetHeat() {
        FiringPlan testFiringPlan = new FiringPlan(mockTarget);

        // Firing plan with no weapons.
        int expectedHeat = 0;
        int actualHeat = testFiringPlan.getHeat();
        TestCase.assertEquals(expectedHeat, actualHeat);

        // Fire just the PPC.
        testFiringPlan.addWeaponFire(mockPPCInfo);
        expectedHeat = 10;
        actualHeat = testFiringPlan.getHeat();
        TestCase.assertEquals(expectedHeat, actualHeat);

        // Add in the LRM.
        testFiringPlan.addWeaponFire(mockLRM20Info);
        expectedHeat = 17;
        actualHeat = testFiringPlan.getHeat();
        TestCase.assertEquals(expectedHeat, actualHeat);

        // Reset and test adding all weapons at once.
        FiringPlan testGroupFiringPlan = new FiringPlan(mockTarget);
        List<WeaponFireInfo> testList = new ArrayList<WeaponFireInfo>(3);
        testList.add(mockMediumLaserInfo);
        testList.add(mockPPCInfo);
        testList.add(mockLRM20Info);
        testGroupFiringPlan.addWeaponFireList(testList);
        expectedHeat = 20;
        actualHeat = testGroupFiringPlan.getHeat();
        TestCase.assertEquals(expectedHeat, actualHeat);
    }

    @Test
    public void testGetExpectedDamage() {
        FiringPlan testFiringPlan = new FiringPlan(mockTarget);
        final double DELTA = 0.001;

        // Firing plan with no weapons.
        double expectedDmg = 0;
        double actualDmg = testFiringPlan.getExpectedDamage();
        TestCase.assertEquals(expectedDmg, actualDmg, DELTA);

        // Fire just the medium laser.
        testFiringPlan.addWeaponFire(mockMediumLaserInfo);
        expectedDmg = mockMediumLaserInfo.getExpectedDamage();
        actualDmg = testFiringPlan.getExpectedDamage();
        TestCase.assertEquals(expectedDmg, actualDmg, DELTA);

        // Add the LRM.
        testFiringPlan.addWeaponFire(mockLRM20Info);
        expectedDmg += mockLRM20Info.getExpectedDamage();
        actualDmg = testFiringPlan.getExpectedDamage();
        TestCase.assertEquals(expectedDmg, actualDmg, DELTA);

        // Add the PPC.
        testFiringPlan.addWeaponFire(mockPPCInfo);
        expectedDmg += mockPPCInfo.getExpectedDamage();
        actualDmg = testFiringPlan.getExpectedDamage();
        TestCase.assertEquals(expectedDmg, actualDmg, DELTA);

        // Reset and test adding all weapons at once.
        FiringPlan testGroupFiringPlan = new FiringPlan(mockTarget);
        List<WeaponFireInfo> testList = new ArrayList<WeaponFireInfo>(3);
        testList.add(mockMediumLaserInfo);
        testList.add(mockPPCInfo);
        testList.add(mockLRM20Info);
        testGroupFiringPlan.addWeaponFireList(testList);
        expectedDmg = mockMediumLaserInfo.getExpectedDamage() + mockPPCInfo.getExpectedDamage() +
                      mockLRM20Info.getExpectedDamage();
        actualDmg = testGroupFiringPlan.getExpectedDamage();
        TestCase.assertEquals(expectedDmg, actualDmg);
    }

    @Test
    public void testGetExpectedCriticals() {
        FiringPlan testFiringPlan = new FiringPlan(mockTarget);
        final double DELTA = 0.0001;

        // Firing plan with no weapons.
        double expectedCrits = 0.0;
        double actualCrits = testFiringPlan.getExpectedCriticals();
        TestCase.assertEquals(expectedCrits, actualCrits, DELTA);

        // Fire just the medium laser.
        testFiringPlan.addWeaponFire(mockMediumLaserInfo);
        expectedCrits += mockMediumLaserInfo.getExpectedCriticals();
        actualCrits = testFiringPlan.getExpectedCriticals();
        TestCase.assertEquals(expectedCrits, actualCrits, DELTA);

        // Add the PPC.
        testFiringPlan.addWeaponFire(mockPPCInfo);
        expectedCrits += mockPPCInfo.getExpectedCriticals();
        actualCrits = testFiringPlan.getExpectedCriticals();
        TestCase.assertEquals(expectedCrits, actualCrits, DELTA);

        // Add the LRM.
        testFiringPlan.addWeaponFire(mockLRM20Info);
        expectedCrits += mockLRM20Info.getExpectedCriticals();
        actualCrits = testFiringPlan.getExpectedCriticals();
        TestCase.assertEquals(expectedCrits, actualCrits, DELTA);

        // Reset and test adding all weapons at once.
        FiringPlan testGroupFiringPlan = new FiringPlan(mockTarget);
        List<WeaponFireInfo> testList = new ArrayList<WeaponFireInfo>(3);
        testList.add(mockMediumLaserInfo);
        testList.add(mockPPCInfo);
        testList.add(mockLRM20Info);
        testGroupFiringPlan.addWeaponFireList(testList);
        expectedCrits = mockMediumLaserInfo.getExpectedCriticals() + mockPPCInfo.getExpectedCriticals() +
                      mockLRM20Info.getExpectedCriticals();
        actualCrits = testGroupFiringPlan.getExpectedCriticals();
        TestCase.assertEquals(expectedCrits, actualCrits, DELTA);
    }

    @Test
    public void testGetKillProbability() {
        FiringPlan testFiringPlan = new FiringPlan(mockTarget);
        final double DELTA = 0.0001;

        // Firing plan with no weapons.
        double expectedProb = 0.0;
        double actualProb = testFiringPlan.getKillProbability();
        TestCase.assertEquals(expectedProb, actualProb, DELTA);

        // Add the PPC.
        testFiringPlan.addWeaponFire(mockPPCInfo);
        expectedProb += mockPPCInfo.getKillProbability();
        actualProb = testFiringPlan.getKillProbability();
        TestCase.assertEquals(expectedProb, actualProb, DELTA);

        // Add the medium laser.
        testFiringPlan.addWeaponFire(mockMediumLaserInfo);
        expectedProb += mockMediumLaserInfo.getKillProbability();
        actualProb = testFiringPlan.getKillProbability();
        TestCase.assertEquals(expectedProb, actualProb, DELTA);

        // Add the LRM.
        testFiringPlan.addWeaponFire(mockLRM20Info);
        expectedProb += mockLRM20Info.getKillProbability();
        actualProb = testFiringPlan.getKillProbability();
        TestCase.assertEquals(expectedProb, actualProb, DELTA);

        // Reset and test adding all weapons at once.
        FiringPlan testGroupFiringPlan = new FiringPlan(mockTarget);
        List<WeaponFireInfo> testList = new ArrayList<WeaponFireInfo>(3);
        testList.add(mockMediumLaserInfo);
        testList.add(mockPPCInfo);
        testList.add(mockLRM20Info);
        testGroupFiringPlan.addWeaponFireList(testList);
        expectedProb = mockMediumLaserInfo.getKillProbability() + mockPPCInfo.getKillProbability() +
                      mockLRM20Info.getKillProbability();
        actualProb = testGroupFiringPlan.getKillProbability();
        TestCase.assertEquals(expectedProb, actualProb, DELTA);
    }

    @Test
    public void testHasWeapon() {
        FiringPlan testFiringPlan = new FiringPlan(mockTarget);

        // Firing plan with no weapons.
        TestCase.assertFalse(testFiringPlan.containsWeapon(null));
        TestCase.assertFalse(testFiringPlan.containsWeapon(mockMediumLaser));
        TestCase.assertFalse(testFiringPlan.containsWeapon(mockPPC));
        TestCase.assertFalse(testFiringPlan.containsWeapon(mockLRM20));

        // Add the LRM.
        testFiringPlan.addWeaponFire(mockLRM20Info);
        TestCase.assertFalse(testFiringPlan.containsWeapon(null));
        TestCase.assertFalse(testFiringPlan.containsWeapon(mockMediumLaser));
        TestCase.assertFalse(testFiringPlan.containsWeapon(mockPPC));
        TestCase.assertTrue(testFiringPlan.containsWeapon(mockLRM20));

        // Add the PPC.
        testFiringPlan.addWeaponFire(mockPPCInfo);
        TestCase.assertFalse(testFiringPlan.containsWeapon(null));
        TestCase.assertFalse(testFiringPlan.containsWeapon(mockMediumLaser));
        TestCase.assertTrue(testFiringPlan.containsWeapon(mockPPC));
        TestCase.assertTrue(testFiringPlan.containsWeapon(mockLRM20));

        // Add the medium laser.
        testFiringPlan.addWeaponFire(mockMediumLaserInfo);
        TestCase.assertFalse(testFiringPlan.containsWeapon(null));
        TestCase.assertTrue(testFiringPlan.containsWeapon(mockMediumLaser));
        TestCase.assertTrue(testFiringPlan.containsWeapon(mockPPC));
        TestCase.assertTrue(testFiringPlan.containsWeapon(mockLRM20));

        // Reset and test adding all weapons at once.
        FiringPlan testGroupFiringPlan = new FiringPlan(mockTarget);
        List<WeaponFireInfo> testList = new ArrayList<WeaponFireInfo>(3);
        testList.add(mockMediumLaserInfo);
        testList.add(mockPPCInfo);
        testList.add(mockLRM20Info);
        testGroupFiringPlan.addWeaponFireList(testList);
        TestCase.assertFalse(testGroupFiringPlan.containsWeapon(null));
        TestCase.assertTrue(testGroupFiringPlan.containsWeapon(mockMediumLaser));
        TestCase.assertTrue(testGroupFiringPlan.containsWeapon(mockPPC));
        TestCase.assertTrue(testGroupFiringPlan.containsWeapon(mockLRM20));
    }

    @Test
    public void testGetEntityActionVector() {
        FiringPlan testFiringPlan = new FiringPlan(mockTarget);

        // Firing plan with no weapons.
        Vector<EntityAction> expectedVector = new Vector<EntityAction>(0);
        Vector<EntityAction> actualVector = testFiringPlan.getEntityActionVector();
        TestCase.assertEquals(expectedVector, actualVector);

        // Add the medium laser.
        expectedVector = new Vector<EntityAction>(1);
        expectedVector.add(mockMediumLaserInfo.getWeaponAttackAction());
        testFiringPlan.addWeaponFire(mockMediumLaserInfo);
        actualVector = testFiringPlan.getEntityActionVector();
        TestCase.assertEquals(expectedVector, actualVector);

        // Add a torso twist.
        int leftTwist = -1;
        expectedVector = new Vector<EntityAction>(2);
        expectedVector.add(new TorsoTwistAction(mockShooter.getId(),
                                                FireControl.correctFacing(mockShooter.getFacing() + leftTwist)));
        expectedVector.add(mockMediumLaserInfo.getWeaponAttackAction());
        testFiringPlan.setTwist(leftTwist);
        actualVector = testFiringPlan.getEntityActionVector();
        TestCase.assertEquals(expectedVector, actualVector);

        // Add a right twist and the PPC.
        int rightTwist = 1;
        expectedVector = new Vector<EntityAction>(3);
        expectedVector.add(new TorsoTwistAction(mockShooter.getId(),
                                                FireControl.correctFacing(mockShooter.getFacing() + rightTwist)));
        expectedVector.add(mockMediumLaserInfo.getWeaponAttackAction());
        expectedVector.add(mockPPCInfo.getWeaponAttackAction());
        testFiringPlan.setTwist(rightTwist);
        testFiringPlan.addWeaponFire(mockPPCInfo);
        actualVector = testFiringPlan.getEntityActionVector();
        TestCase.assertEquals(expectedVector, actualVector);

        // Add the LRM and set no twist.
        expectedVector = new Vector<EntityAction>(3);
        expectedVector.add(mockMediumLaserInfo.getWeaponAttackAction());
        expectedVector.add(mockPPCInfo.getWeaponAttackAction());
        expectedVector.add(mockLRM20Info.getWeaponAttackAction());
        testFiringPlan.setTwist(0);
        testFiringPlan.addWeaponFire(mockLRM20Info);
        actualVector = testFiringPlan.getEntityActionVector();
        TestCase.assertEquals(expectedVector, actualVector);

        // Reset and add all weapons at once with a left twist.
        expectedVector = new Vector<EntityAction>(4);
        expectedVector.add(new TorsoTwistAction(mockShooter.getId(),
                                                FireControl.correctFacing(mockShooter.getFacing() + leftTwist)));
        expectedVector.add(mockMediumLaserInfo.getWeaponAttackAction());
        expectedVector.add(mockPPCInfo.getWeaponAttackAction());
        expectedVector.add(mockLRM20Info.getWeaponAttackAction());
        testFiringPlan = new FiringPlan(mockTarget);
        testFiringPlan.setTwist(leftTwist);
        List<WeaponFireInfo> testList = new ArrayList<WeaponFireInfo>(3);
        testList.add(mockMediumLaserInfo);
        testList.add(mockPPCInfo);
        testList.add(mockLRM20Info);
        testFiringPlan.addWeaponFireList(testList);
        actualVector = testFiringPlan.getEntityActionVector();
        TestCase.assertEquals(expectedVector, actualVector);
    }

    @Test
    public void testGetDebugDescription() {
        final NumberFormat LOG_PER = NumberFormat.getPercentInstance();
        final NumberFormat LOG_DEC = DecimalFormat.getInstance();

        FiringPlan testFiringePlan = new FiringPlan(mockTarget);

        // Test an empty firing plan.
        String expectedDebug = "Empty FiringPlan!";
        String actualDebug = testFiringePlan.getDebugDescription(false);
        TestCase.assertEquals(expectedDebug, actualDebug);
        actualDebug = testFiringePlan.getDebugDescription(true);
        TestCase.assertEquals(expectedDebug, actualDebug);

        // Add the medium laser, undetailed.
        testFiringePlan.addWeaponFire(mockMediumLaserInfo);
        StringBuilder expectedDescription = new StringBuilder("Firing Plan for ").append(mockShooter.getDisplayName());
        expectedDescription.append(" at ").append(mockTarget.getDisplayName());
        expectedDescription.append(" ").append(1).append(" weapons fired \n");
        expectedDescription.append("Total Expected Damage=")
                           .append(LOG_DEC.format(mockMediumLaserInfo.getExpectedDamage()))
                           .append("\n");
        expectedDescription.append("Total Expected Criticals=")
                           .append(LOG_DEC.format(mockMediumLaserInfo.getExpectedCriticals()))
                           .append("\n");
        expectedDescription.append("Kill Probability=")
                           .append(LOG_PER.format(mockMediumLaserInfo.getKillProbability()));
        String actualDescription = testFiringePlan.getDebugDescription(false);
        TestCase.assertEquals(expectedDescription.toString(), actualDescription);

        // Detailed.
        expectedDescription = new StringBuilder("Firing Plan for ").append(mockShooter.getDisplayName());
        expectedDescription.append(" at ").append(mockTarget.getDisplayName());
        expectedDescription.append(" ").append(1).append(" weapons fired \n");
        expectedDescription.append(mockMediumLaserInfo.getDebugDescription()).append("\n");
        expectedDescription.append("Total Expected Damage=")
                           .append(LOG_DEC.format(mockMediumLaserInfo.getExpectedDamage()))
                           .append("\n");
        expectedDescription.append("Total Expected Criticals=")
                           .append(LOG_DEC.format(mockMediumLaserInfo.getExpectedCriticals()))
                           .append("\n");
        expectedDescription.append("Kill Probability=")
                           .append(LOG_PER.format(mockMediumLaserInfo.getKillProbability()));
        actualDescription = testFiringePlan.getDebugDescription(true);
        TestCase.assertEquals(expectedDescription.toString(), actualDescription);

        // Add the PPC, undetailed.
        testFiringePlan.addWeaponFire(mockPPCInfo);
        expectedDescription = new StringBuilder("Firing Plan for ").append(mockShooter.getDisplayName());
        expectedDescription.append(" at ").append(mockTarget.getDisplayName());
        expectedDescription.append(" ").append(2).append(" weapons fired \n");
        double dmg = mockMediumLaserInfo.getExpectedDamage() + mockPPCInfo.getExpectedDamage();
        expectedDescription.append("Total Expected Damage=")
                           .append(LOG_DEC.format(dmg))
                           .append("\n");
        double crits = mockMediumLaserInfo.getExpectedCriticals() + mockPPCInfo.getExpectedCriticals();
        expectedDescription.append("Total Expected Criticals=")
                           .append(LOG_DEC.format(crits))
                           .append("\n");
        double kill = mockMediumLaserInfo.getKillProbability() + mockPPCInfo.getKillProbability();
        expectedDescription.append("Kill Probability=")
                           .append(LOG_PER.format(kill));
        actualDescription = testFiringePlan.getDebugDescription(false);
        TestCase.assertEquals(expectedDescription.toString(), actualDescription);

        // Detailed.
        expectedDescription = new StringBuilder("Firing Plan for ").append(mockShooter.getDisplayName());
        expectedDescription.append(" at ").append(mockTarget.getDisplayName());
        expectedDescription.append(" ").append(2).append(" weapons fired \n");
        expectedDescription.append(mockMediumLaserInfo.getDebugDescription()).append("\n");
        expectedDescription.append(mockPPCInfo.getDebugDescription()).append("\n");
        dmg = mockMediumLaserInfo.getExpectedDamage() + mockPPCInfo.getExpectedDamage();
        expectedDescription.append("Total Expected Damage=")
                           .append(LOG_DEC.format(dmg))
                           .append("\n");
        crits = mockMediumLaserInfo.getExpectedCriticals() + mockPPCInfo.getExpectedCriticals();
        expectedDescription.append("Total Expected Criticals=")
                           .append(LOG_DEC.format(crits))
                           .append("\n");
        kill = mockMediumLaserInfo.getKillProbability() + mockPPCInfo.getKillProbability();
        expectedDescription.append("Kill Probability=")
                           .append(LOG_PER.format(kill));
        actualDescription = testFiringePlan.getDebugDescription(true);
        TestCase.assertEquals(expectedDescription.toString(), actualDescription);

        // Add the LRM, undetailed.
        testFiringePlan.addWeaponFire(mockLRM20Info);
        expectedDescription = new StringBuilder("Firing Plan for ").append(mockShooter.getDisplayName());
        expectedDescription.append(" at ").append(mockTarget.getDisplayName());
        expectedDescription.append(" ").append(3).append(" weapons fired \n");
        dmg = mockMediumLaserInfo.getExpectedDamage() + mockPPCInfo.getExpectedDamage() +
              mockLRM20Info.getExpectedDamage();
        expectedDescription.append("Total Expected Damage=")
                           .append(LOG_DEC.format(dmg))
                           .append("\n");
        crits = mockMediumLaserInfo.getExpectedCriticals() + mockPPCInfo.getExpectedCriticals() +
                mockLRM20Info.getExpectedCriticals();
        expectedDescription.append("Total Expected Criticals=")
                           .append(LOG_DEC.format(crits))
                           .append("\n");
        kill = mockMediumLaserInfo.getKillProbability() + mockPPCInfo.getKillProbability() +
               mockLRM20Info.getKillProbability();
        expectedDescription.append("Kill Probability=")
                           .append(LOG_PER.format(kill));
        actualDescription = testFiringePlan.getDebugDescription(false);
        TestCase.assertEquals(expectedDescription.toString(), actualDescription);

        // Detailed.
        expectedDescription = new StringBuilder("Firing Plan for ").append(mockShooter.getDisplayName());
        expectedDescription.append(" at ").append(mockTarget.getDisplayName());
        expectedDescription.append(" ").append(3).append(" weapons fired \n");
        expectedDescription.append(mockMediumLaserInfo.getDebugDescription()).append("\n");
        expectedDescription.append(mockPPCInfo.getDebugDescription()).append("\n");
        expectedDescription.append(mockLRM20Info.getDebugDescription()).append("\n");
        dmg = mockMediumLaserInfo.getExpectedDamage() + mockPPCInfo.getExpectedDamage() +
              mockLRM20Info.getExpectedDamage();
        expectedDescription.append("Total Expected Damage=")
                           .append(LOG_DEC.format(dmg))
                           .append("\n");
        crits = mockMediumLaserInfo.getExpectedCriticals() + mockPPCInfo.getExpectedCriticals() +
                mockLRM20Info.getExpectedCriticals();
        expectedDescription.append("Total Expected Criticals=")
                           .append(LOG_DEC.format(crits))
                           .append("\n");
        kill = mockMediumLaserInfo.getKillProbability() + mockPPCInfo.getKillProbability() +
               mockLRM20Info.getKillProbability();
        expectedDescription.append("Kill Probability=")
                           .append(LOG_PER.format(kill));
        actualDescription = testFiringePlan.getDebugDescription(true);
        TestCase.assertEquals(expectedDescription.toString(), actualDescription);

        // Reset and add all weapons at once, undetailed.
        testFiringePlan = new FiringPlan(mockTarget);
        List<WeaponFireInfo> testList = new ArrayList<WeaponFireInfo>(3);
        testList.add(mockMediumLaserInfo);
        testList.add(mockPPCInfo);
        testList.add(mockLRM20Info);
        testFiringePlan.addWeaponFireList(testList);
        expectedDescription = new StringBuilder("Firing Plan for ").append(mockShooter.getDisplayName());
        expectedDescription.append(" at ").append(mockTarget.getDisplayName());
        expectedDescription.append(" ").append(3).append(" weapons fired \n");
        dmg = 0.0;
        crits = 0.0;
        kill = 0.0;
        for (WeaponFireInfo wfi : testList) {
            dmg += wfi.getExpectedDamage();
            crits += wfi.getExpectedCriticals();
            kill += wfi.getKillProbability();
        }
        expectedDescription.append("Total Expected Damage=")
                           .append(LOG_DEC.format(dmg))
                           .append("\n");
        expectedDescription.append("Total Expected Criticals=")
                           .append(LOG_DEC.format(crits))
                           .append("\n");
        expectedDescription.append("Kill Probability=")
                           .append(LOG_PER.format(kill));
        actualDescription = testFiringePlan.getDebugDescription(false);
        TestCase.assertEquals(expectedDescription.toString(), actualDescription);

        // Reset and add all weapons at once, detailed.
        testFiringePlan = new FiringPlan(mockTarget);
        testList = new ArrayList<WeaponFireInfo>(3);
        testList.add(mockMediumLaserInfo);
        testList.add(mockPPCInfo);
        testList.add(mockLRM20Info);
        testFiringePlan.addWeaponFireList(testList);
        expectedDescription = new StringBuilder("Firing Plan for ").append(mockShooter.getDisplayName());
        expectedDescription.append(" at ").append(mockTarget.getDisplayName());
        expectedDescription.append(" ").append(3).append(" weapons fired \n");
        dmg = 0.0;
        crits = 0.0;
        kill = 0.0;
        for (WeaponFireInfo wfi : testList) {
            dmg += wfi.getExpectedDamage();
            crits += wfi.getExpectedCriticals();
            kill += wfi.getKillProbability();
            expectedDescription.append(wfi.getDebugDescription()).append("\n");
        }
        expectedDescription.append("Total Expected Damage=")
                           .append(LOG_DEC.format(dmg))
                           .append("\n");
        expectedDescription.append("Total Expected Criticals=")
                           .append(LOG_DEC.format(crits))
                           .append("\n");
        expectedDescription.append("Kill Probability=")
                           .append(LOG_PER.format(kill));
        actualDescription = testFiringePlan.getDebugDescription(true);
        TestCase.assertEquals(expectedDescription.toString(), actualDescription);
    }

    @Test
    public void testCalcUtility() {
        FiringPlan testFiringPlan = new FiringPlan(mockTarget);
        final double DELTA = 0.001;

        // Firing plan with no weapons.
        double expectedUtility = 0;
        int OVERHEAT = 14;
        testFiringPlan.calcUtility(OVERHEAT);
        double actualUtility = testFiringPlan.getUtility();
        TestCase.assertEquals(expectedUtility, actualUtility, DELTA);

        // Fire the medium laser.
        testFiringPlan.addWeaponFire(mockMediumLaserInfo);
        expectedUtility = 2.915;
        testFiringPlan.calcUtility(OVERHEAT);
        actualUtility = testFiringPlan.getUtility();
        TestCase.assertEquals(expectedUtility, actualUtility, DELTA);

        // Add the PPC.
        testFiringPlan.addWeaponFire(mockPPCInfo);
        expectedUtility = 9.5685;
        testFiringPlan.calcUtility(OVERHEAT);
        actualUtility = testFiringPlan.getUtility();
        TestCase.assertEquals(expectedUtility, actualUtility, DELTA);

        // Add the LRM.
        testFiringPlan.addWeaponFire(mockLRM20Info);
        expectedUtility = -12.841064374;
        testFiringPlan.calcUtility(OVERHEAT);
        actualUtility = testFiringPlan.getUtility();
        TestCase.assertEquals(expectedUtility, actualUtility, DELTA);

        // Reset and test adding all weapons at once.
        FiringPlan testGroupFiringPlan = new FiringPlan(mockTarget);
        List<WeaponFireInfo> testList = new ArrayList<WeaponFireInfo>(3);
        testList.add(mockMediumLaserInfo);
        testList.add(mockPPCInfo);
        testList.add(mockLRM20Info);
        testGroupFiringPlan.addWeaponFireList(testList);
        expectedUtility = -12.841064374;
        testGroupFiringPlan.calcUtility(OVERHEAT);
        actualUtility = testGroupFiringPlan.getUtility();
        TestCase.assertEquals(expectedUtility, actualUtility, DELTA);
    }
}

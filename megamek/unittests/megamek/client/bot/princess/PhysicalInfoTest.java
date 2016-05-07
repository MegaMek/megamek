package megamek.client.bot.princess;

import megamek.common.BipedMech;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Mech;
import megamek.common.QuadMech;
import megamek.common.Tank;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.actions.KickAttackAction;
import megamek.common.actions.PunchAttackAction;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @version %Id%
 * @since 2/27/14 3:23 PM
 */
@RunWith(JUnit4.class)
public class PhysicalInfoTest {

    private static final double TOLERANCE = 0.0001;

    @Test
    public void testInitDamage() {
        Princess mockPrincess = Mockito.mock(Princess.class);

        FireControl mockFireControl = Mockito.mock(FireControl.class);
        Mockito.when(mockPrincess.getFireControl()).thenReturn(mockFireControl);

        ToHitData mockToHit = Mockito.mock(ToHitData.class);
        Mockito.when(mockFireControl.guessToHitModifierPhysical(Mockito.any(Entity.class),
                                                                Mockito.any(EntityState.class),
                                                                Mockito.any(Targetable.class),
                                                                Mockito.any(EntityState.class),
                                                                Mockito.any(PhysicalAttackType.class),
                                                                Mockito.any(IGame.class)))
               .thenReturn(mockToHit);
        Mockito.when(mockToHit.getValue()).thenReturn(7);

        Entity mockShooter = Mockito.mock(BipedMech.class);
        Mockito.when(mockShooter.getId()).thenReturn(1);
        Mockito.when(mockShooter.getWeight()).thenReturn(50.0);

        EntityState mockShooterState = Mockito.mock(EntityState.class);

        Mech mockTarget = Mockito.mock(BipedMech.class);
        Mockito.when(mockTarget.isLocationBad(Mockito.anyInt())).thenReturn(false);
        Mockito.when(mockTarget.getArmor(Mockito.anyInt(), Mockito.eq(false))).thenReturn(10);
        Mockito.when(mockTarget.getArmor(Mockito.anyInt(), Mockito.eq(true))).thenReturn(5);
        Mockito.when(mockTarget.getInternal(Mockito.anyInt())).thenReturn(6);

        EntityState mockTargetState = Mockito.mock(EntityState.class);

        IGame mockGame = Mockito.mock(IGame.class);

        PhysicalInfo testPhysicalInfo = Mockito.spy(new PhysicalInfo(mockPrincess));
        testPhysicalInfo.setShooter(mockShooter);
        testPhysicalInfo.setTarget(mockTarget);
        Mockito.doNothing().when(testPhysicalInfo).setDamageDirection(Mockito.any(EntityState.class),
                                                                      Mockito.any(Coords.class));
        Mockito.doReturn(1).when(testPhysicalInfo).getDamageDirection();

        PhysicalAttackType punch = PhysicalAttackType.LEFT_PUNCH;
        PhysicalAttackType kick = PhysicalAttackType.LEFT_KICK;

        PunchAttackAction punchAction = Mockito.mock(PunchAttackAction.class);
        Mockito.doReturn(punchAction).when(testPhysicalInfo).buildAction(Mockito.eq(punch), Mockito.anyInt(),
                                                                         Mockito.any(Targetable.class));
        Mockito.when(punchAction.toHit(Mockito.any(IGame.class))).thenReturn(mockToHit);

        KickAttackAction kickAction = Mockito.mock(KickAttackAction.class);
        Mockito.doReturn(kickAction).when(testPhysicalInfo).buildAction(Mockito.eq(kick), Mockito.anyInt(),
                                                                        Mockito.any(Targetable.class));
        Mockito.when(kickAction.toHit(Mockito.any(IGame.class))).thenReturn(mockToHit);

        // Test a vanilla punch.
        testPhysicalInfo.setShooter(mockShooter);
        testPhysicalInfo.setAttackType(punch);
        testPhysicalInfo.initDamage(punch, mockShooterState, mockTargetState, true, mockGame);
        Assert.assertEquals(0.583, testPhysicalInfo.getProbabilityToHit(), TOLERANCE);
        Assert.assertEquals(5.0, testPhysicalInfo.getMaxDamage(), TOLERANCE);
        Assert.assertEquals(0.0099, testPhysicalInfo.getExpectedCriticals(), TOLERANCE);
        Assert.assertEquals(0.0, testPhysicalInfo.getKillProbability(), TOLERANCE);
        Assert.assertEquals(5.0, testPhysicalInfo.getExpectedDamageOnHit(), TOLERANCE);

        // Test a vanilla kick.
        testPhysicalInfo.setShooter(mockShooter);
        testPhysicalInfo.setAttackType(kick);
        testPhysicalInfo.initDamage(kick, mockShooterState, mockTargetState, true, mockGame);
        Assert.assertEquals(0.583, testPhysicalInfo.getProbabilityToHit(), TOLERANCE);
        Assert.assertEquals(10.0, testPhysicalInfo.getMaxDamage(), TOLERANCE);
        Assert.assertEquals(0.0099, testPhysicalInfo.getExpectedCriticals(), TOLERANCE);
        Assert.assertEquals(0.0, testPhysicalInfo.getKillProbability(), TOLERANCE);
        Assert.assertEquals(10.0, testPhysicalInfo.getExpectedDamageOnHit(), TOLERANCE);

        // Make the puncher heavier.
        Mockito.when(mockShooter.getWeight()).thenReturn(100.0);
        testPhysicalInfo.setShooter(mockShooter);
        testPhysicalInfo.setAttackType(punch);
        testPhysicalInfo.initDamage(punch, mockShooterState, mockTargetState, true, mockGame);
        Assert.assertEquals(0.583, testPhysicalInfo.getProbabilityToHit(), TOLERANCE);
        Assert.assertEquals(10.0, testPhysicalInfo.getMaxDamage(), TOLERANCE);
        Assert.assertEquals(0.0099, testPhysicalInfo.getExpectedCriticals(), TOLERANCE);
        Assert.assertEquals(0.0, testPhysicalInfo.getKillProbability(), TOLERANCE);
        Assert.assertEquals(10.0, testPhysicalInfo.getExpectedDamageOnHit(), TOLERANCE);

        // Give the target less armor and internals
        Mockito.when(mockTarget.isLocationBad(Mockito.anyInt())).thenReturn(false);
        Mockito.when(mockTarget.getArmor(Mockito.anyInt(), Mockito.eq(false))).thenReturn(6);
        Mockito.when(mockTarget.getArmor(Mockito.anyInt(), Mockito.eq(true))).thenReturn(3);
        Mockito.when(mockTarget.getInternal(Mockito.anyInt())).thenReturn(3);
        Mockito.when(mockShooter.getWeight()).thenReturn(100.0);
        testPhysicalInfo.setShooter(mockShooter);
        testPhysicalInfo.setAttackType(punch);
        testPhysicalInfo.initDamage(punch, mockShooterState, mockTargetState, true, mockGame);
        Assert.assertEquals(0.583, testPhysicalInfo.getProbabilityToHit(), TOLERANCE);
        Assert.assertEquals(10.0, testPhysicalInfo.getMaxDamage(), TOLERANCE);
        Assert.assertEquals(0.5929, testPhysicalInfo.getExpectedCriticals(), TOLERANCE);
        Assert.assertEquals(0.1943, testPhysicalInfo.getKillProbability(), TOLERANCE);
        Assert.assertEquals(10.0, testPhysicalInfo.getExpectedDamageOnHit(), TOLERANCE);

        // Test a non-biped trying to punch.
        testPhysicalInfo.setShooter(Mockito.mock(QuadMech.class));
        testPhysicalInfo.initDamage(punch, mockShooterState, mockTargetState, true, mockGame);
        Assert.assertEquals(0.0, testPhysicalInfo.getProbabilityToHit(), TOLERANCE);
        Assert.assertEquals(0.0, testPhysicalInfo.getMaxDamage(), TOLERANCE);
        Assert.assertEquals(0.0, testPhysicalInfo.getExpectedCriticals(), TOLERANCE);
        Assert.assertEquals(0.0, testPhysicalInfo.getKillProbability(), TOLERANCE);
        Assert.assertEquals(0.0, testPhysicalInfo.getExpectedDamageOnHit(), TOLERANCE);

        // Test not being able to hit.
        Mockito.when(mockToHit.getValue()).thenReturn(13);
        testPhysicalInfo.initDamage(punch, mockShooterState, mockTargetState, true, mockGame);
        Assert.assertEquals(0.0, testPhysicalInfo.getProbabilityToHit(), TOLERANCE);
        Assert.assertEquals(0.0, testPhysicalInfo.getMaxDamage(), TOLERANCE);
        Assert.assertEquals(0.0, testPhysicalInfo.getExpectedCriticals(), TOLERANCE);
        Assert.assertEquals(0.0, testPhysicalInfo.getKillProbability(), TOLERANCE);
        Assert.assertEquals(0.0, testPhysicalInfo.getExpectedDamageOnHit(), TOLERANCE);

        // Test a non-mech.
        testPhysicalInfo.setShooter(Mockito.mock(Tank.class));
        testPhysicalInfo.initDamage(punch, mockShooterState, mockTargetState, true, mockGame);
        Assert.assertEquals(0.0, testPhysicalInfo.getProbabilityToHit(), TOLERANCE);
        Assert.assertEquals(0.0, testPhysicalInfo.getMaxDamage(), TOLERANCE);
        Assert.assertEquals(0.0, testPhysicalInfo.getExpectedCriticals(), TOLERANCE);
        Assert.assertEquals(0.0, testPhysicalInfo.getKillProbability(), TOLERANCE);
        Assert.assertEquals(0.0, testPhysicalInfo.getExpectedDamageOnHit(), TOLERANCE);
    }
}

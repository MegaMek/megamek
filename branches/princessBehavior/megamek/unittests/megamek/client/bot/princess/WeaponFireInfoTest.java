package megamek.client.bot.princess;

import junit.framework.TestCase;
import megamek.common.BipedMech;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.IGame;
import megamek.common.Mounted;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.util.LogLevel;
import megamek.common.util.Logger;
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
 * @since 9/14/13 10:43 PM
 */
@RunWith(JUnit4.class)
public class WeaponFireInfoTest {

    private final int SHOOTER_ID = 1;
    private final int TARGET_ID = 2;
    private final int WEAPON_ID = 3;

    private final Coords SHOOTER_COORDS = new Coords(10, 10);

    private final Coords TARGET_COORDS_3 = new Coords(10,13);
    private final Coords TARGET_COORDS_9 = new Coords(10,19);
    private final Coords TARGET_COORDS_18 = new Coords(10, 28);
    private final Coords TARGET_COORDS_24 = new Coords(10, 34);
    private final int TARGET_FACING = 3;

    ToHitData mockToHitEight = Mockito.mock(ToHitData.class);
    ToHitData mockToHitSix = Mockito.mock(ToHitData.class);
    ToHitData mockToHitThirteen = Mockito.mock(ToHitData.class);
    BipedMech mockShooter      = Mockito.mock(BipedMech.class);
    EntityState mockShooterState = Mockito.mock(EntityState.class);
    BipedMech  mockTarget       = Mockito.mock(BipedMech.class);
    EntityState mockTargetState  = Mockito.mock(EntityState.class);
    IGame mockGame         = Mockito.mock(IGame.class);
    Mounted mockWeapon = Mockito.mock(Mounted.class);
    WeaponType mockWeaponType = Mockito.mock(WeaponType.class);
    WeaponAttackAction mockWeaponAttackAction = Mockito.mock(WeaponAttackAction.class);

    @Before
    public void setUp() {
        Logger.setVerbosity(LogLevel.DEBUG);

        Mockito.when(mockToHitSix.getValue()).thenReturn(6);
        Mockito.when(mockToHitEight.getValue()).thenReturn(8);
        Mockito.when(mockToHitThirteen.getValue()).thenReturn(ToHitData.AUTOMATIC_FAIL);

        Mockito.when(mockShooter.getPosition()).thenReturn(SHOOTER_COORDS);
        Mockito.when(mockShooter.getWeight()).thenReturn(75.0f);
        Mockito.when(mockShooter.getId()).thenReturn(SHOOTER_ID);
        Mockito.when(mockShooter.getDisplayName()).thenReturn("Test shooter 1");
        Mockito.when(mockShooter.getEquipmentNum(Mockito.eq(mockWeapon))).thenReturn(WEAPON_ID);
        Mockito.when(mockShooterState.getPosition()).thenReturn(SHOOTER_COORDS);

        Mockito.when(mockTarget.getPosition()).thenReturn(TARGET_COORDS_9);
        Mockito.when(mockTarget.isLocationBad(Mockito.anyInt())).thenReturn(false);
        Mockito.when(mockTarget.getId()).thenReturn(TARGET_ID);
        Mockito.when(mockTarget.getTargetType()).thenReturn(Targetable.TYPE_ENTITY);
        Mockito.when(mockTarget.getDisplayName()).thenReturn("Mock target 2");
        Mockito.when(mockTargetState.getFacing()).thenReturn(TARGET_FACING);
        Mockito.when(mockTargetState.getPosition()).thenReturn(TARGET_COORDS_9);

        Mockito.when(mockWeapon.getType()).thenReturn(mockWeaponType);
    }

    private void setupLightTarget() {
        Mockito.when(mockTarget.getInternal(Mockito.anyInt())).thenReturn(6);
        Mockito.when(mockTarget.getInternal(Mockito.eq(0))).thenReturn(3);
        Mockito.when(mockTarget.getArmor(Mockito.anyInt(), Mockito.anyBoolean())).thenReturn(12);
        Mockito.when(mockTarget.getArmor(Mockito.eq(0), Mockito.anyBoolean())).thenReturn(4);
    }

    private void setupMediumTarget() {
        Mockito.when(mockTarget.getInternal(Mockito.anyInt())).thenReturn(10);
        Mockito.when(mockTarget.getInternal(Mockito.eq(0))).thenReturn(3);
        Mockito.when(mockTarget.getArmor(Mockito.anyInt(), Mockito.anyBoolean())).thenReturn(16);
        Mockito.when(mockTarget.getArmor(Mockito.eq(0), Mockito.anyBoolean())).thenReturn(9);
    }

    private void setupMediumLaser() {
        Mockito.when(mockWeaponType.getHeat()).thenReturn(3);
        Mockito.when(mockWeaponType.getDamage()).thenReturn(5);
        Mockito.when(mockWeaponType.getShortRange()).thenReturn(3);
        Mockito.when(mockWeaponType.getMediumRange()).thenReturn(6);
        Mockito.when(mockWeaponType.getLongRange()).thenReturn(9);
        Mockito.when(mockWeapon.getDesc()).thenReturn("Medium Laser");
    }

    private void setupPPC() {
        Mockito.when(mockWeaponType.getHeat()).thenReturn(10);
        Mockito.when(mockWeaponType.getDamage()).thenReturn(10);
        Mockito.when(mockWeaponType.getShortRange()).thenReturn(6);
        Mockito.when(mockWeaponType.getMediumRange()).thenReturn(12);
        Mockito.when(mockWeaponType.getLongRange()).thenReturn(18);
        Mockito.when(mockWeapon.getDesc()).thenReturn("PPC");
    }

    private void setupCGR() {
        Mockito.when(mockWeaponType.getHeat()).thenReturn(1);
        Mockito.when(mockWeaponType.getDamage()).thenReturn(15);
        Mockito.when(mockWeaponType.getShortRange()).thenReturn(7);
        Mockito.when(mockWeaponType.getMediumRange()).thenReturn(15);
        Mockito.when(mockWeaponType.getLongRange()).thenReturn(22);
        Mockito.when(mockWeapon.getDesc()).thenReturn("Gauss Rifle (C)");
    }

    @Test
    public void testInitDamage() {
        final double DELTA = 0.000001;

        WeaponFireInfo testWeaponFireInfo = Mockito.spy(new WeaponFireInfo());
        testWeaponFireInfo.setShooter(mockShooter);
        testWeaponFireInfo.setShooterState(mockShooterState);
        testWeaponFireInfo.setTarget(mockTarget);
        testWeaponFireInfo.setTargetState(mockTargetState);
        testWeaponFireInfo.setGame(mockGame);
        testWeaponFireInfo.setWeapon(mockWeapon);

        // Test a medium laser vs light target with a to hit roll of 6.
        setupMediumLaser();
        setupLightTarget();
        double expectedMaxDamage = mockWeaponType.getDamage();
        double expectedProbabilityToHit = Compute.oddsAbove(mockToHitSix.getValue())/100;
        double expectedCriticals = 0.02460;
        double expectedKill = 0;
        Mockito.doReturn(mockToHitSix).when(testWeaponFireInfo).calcToHit();
        Mockito.doReturn(mockWeaponAttackAction).when(testWeaponFireInfo).buildWeaponAttackAction();
        Mockito.doReturn(expectedMaxDamage).when(testWeaponFireInfo).computeExpectedDamage();
        testWeaponFireInfo.initDamage(null, false);
        TestCase.assertEquals(expectedMaxDamage, testWeaponFireInfo.getMaxDamage());
        TestCase.assertEquals(expectedMaxDamage, testWeaponFireInfo.getExpectedDamageOnHit());
        TestCase.assertEquals(expectedProbabilityToHit, testWeaponFireInfo.getProbabilityToHit(), DELTA);
        TestCase.assertEquals(expectedCriticals, testWeaponFireInfo.getExpectedCriticals(), DELTA);
        TestCase.assertEquals(expectedKill, testWeaponFireInfo.getKillProbability(), DELTA);

        // Test a PPC vs light target with a to hit roll of 8.
        setupPPC();
        setupLightTarget();
        expectedMaxDamage = mockWeaponType.getDamage();
        expectedProbabilityToHit = Compute.oddsAbove(mockToHitEight.getValue())/100;
        expectedCriticals = 0.01867;
        expectedKill = 0.01155;
        Mockito.doReturn(mockToHitEight).when(testWeaponFireInfo).calcToHit();
        Mockito.doReturn(mockWeaponAttackAction).when(testWeaponFireInfo).buildWeaponAttackAction();
        Mockito.doReturn(expectedMaxDamage).when(testWeaponFireInfo).computeExpectedDamage();
        testWeaponFireInfo.initDamage(null, false);
        TestCase.assertEquals(expectedMaxDamage, testWeaponFireInfo.getMaxDamage());
        TestCase.assertEquals(expectedMaxDamage, testWeaponFireInfo.getExpectedDamageOnHit());
        TestCase.assertEquals(expectedProbabilityToHit, testWeaponFireInfo.getProbabilityToHit(), DELTA);
        TestCase.assertEquals(expectedCriticals, testWeaponFireInfo.getExpectedCriticals(), DELTA);
        TestCase.assertEquals(expectedKill, testWeaponFireInfo.getKillProbability(), DELTA);

        // Test a Gauss Rifle vs a light target with a to hit roll of 6.
        setupCGR();
        setupLightTarget();
        expectedMaxDamage = mockWeaponType.getDamage();
        expectedProbabilityToHit = Compute.oddsAbove(mockToHitSix.getValue())/100;
        expectedCriticals = 0.46129;
        expectedKill = 0.02005;
        Mockito.doReturn(mockToHitSix).when(testWeaponFireInfo).calcToHit();
        Mockito.doReturn(mockWeaponAttackAction).when(testWeaponFireInfo).buildWeaponAttackAction();
        Mockito.doReturn(expectedMaxDamage).when(testWeaponFireInfo).computeExpectedDamage();
        testWeaponFireInfo.initDamage(null, false);
        TestCase.assertEquals(expectedMaxDamage, testWeaponFireInfo.getMaxDamage());
        TestCase.assertEquals(expectedMaxDamage, testWeaponFireInfo.getExpectedDamageOnHit());
        TestCase.assertEquals(expectedProbabilityToHit, testWeaponFireInfo.getProbabilityToHit(), DELTA);
        TestCase.assertEquals(expectedCriticals, testWeaponFireInfo.getExpectedCriticals(), DELTA);
        TestCase.assertEquals(expectedKill, testWeaponFireInfo.getKillProbability(), DELTA);

        // Test a Gauss Rifle vs. a medium target with a to hit roll of 8.
        setupCGR();
        setupMediumTarget();
        expectedMaxDamage = mockWeaponType.getDamage();
        expectedProbabilityToHit = Compute.oddsAbove(mockToHitEight.getValue())/100;
        expectedCriticals = 0.01867;
        expectedKill = 0.01155;
        Mockito.doReturn(mockToHitEight).when(testWeaponFireInfo).calcToHit();
        Mockito.doReturn(mockWeaponAttackAction).when(testWeaponFireInfo).buildWeaponAttackAction();
        Mockito.doReturn(expectedMaxDamage).when(testWeaponFireInfo).computeExpectedDamage();
        testWeaponFireInfo.initDamage(null, false);
        TestCase.assertEquals(expectedMaxDamage, testWeaponFireInfo.getMaxDamage());
        TestCase.assertEquals(expectedMaxDamage, testWeaponFireInfo.getExpectedDamageOnHit());
        TestCase.assertEquals(expectedProbabilityToHit, testWeaponFireInfo.getProbabilityToHit(), DELTA);
        TestCase.assertEquals(expectedCriticals, testWeaponFireInfo.getExpectedCriticals(), DELTA);
        TestCase.assertEquals(expectedKill, testWeaponFireInfo.getKillProbability(), DELTA);

        // Test a medium laser vs. a medium target with no chance to hit.
        setupMediumLaser();
        setupMediumTarget();
        expectedMaxDamage = 0;
        expectedProbabilityToHit = Compute.oddsAbove(mockToHitThirteen.getValue())/100;
        expectedCriticals = 0;
        expectedKill = 0;
        Mockito.doReturn(mockToHitThirteen).when(testWeaponFireInfo).calcToHit();
        Mockito.doReturn(mockWeaponAttackAction).when(testWeaponFireInfo).buildWeaponAttackAction();
        Mockito.doReturn(expectedMaxDamage).when(testWeaponFireInfo).computeExpectedDamage();
        testWeaponFireInfo.initDamage(null, false);
        TestCase.assertEquals(expectedMaxDamage, testWeaponFireInfo.getMaxDamage());
        TestCase.assertEquals(expectedMaxDamage, testWeaponFireInfo.getExpectedDamageOnHit());
        TestCase.assertEquals(expectedProbabilityToHit, testWeaponFireInfo.getProbabilityToHit(), DELTA);
        TestCase.assertEquals(expectedCriticals, testWeaponFireInfo.getExpectedCriticals(), DELTA);
        TestCase.assertEquals(expectedKill, testWeaponFireInfo.getKillProbability(), DELTA);

        // todo build tests for AeroSpace attacks.
    }

}

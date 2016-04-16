package megamek.client.bot.princess;

import junit.framework.TestCase;
import megamek.common.BipedMech;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Mounted;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

/**
 * Created with IntelliJ IDEA.
 *
 * @version $Id$
 * @lastEditBy Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 12/18/13 1:02 PM
 */
@RunWith(JUnit4.class)
public class WeaponFireInfoTest {

    private final int SHOOTER_ID = 1;
    private final int TARGET_ID = 2;
    private final int WEAPON_ID = 3;

    private final Coords SHOOTER_COORDS = new Coords(10, 10);

    private final Coords TARGET_COORDS_9 = new Coords(10, 19);
    private final int TARGET_FACING = 3;

    private ToHitData mockToHitEight;
    private ToHitData mockToHitSix;
    private ToHitData mockToHitThirteen;
    private BipedMech mockShooter;
    private EntityState mockShooterState;
    private BipedMech mockTarget;
    private EntityState mockTargetState;
    private IGame mockGame;
    private Mounted mockWeapon;
    private WeaponType mockWeaponType;
    private WeaponAttackAction mockWeaponAttackAction;
    private Princess mockPrincess;
    private FireControl mockFireControl;

    @Before
    public void setUp() {
        mockGame = Mockito.mock(IGame.class);

        mockToHitSix = Mockito.mock(ToHitData.class);
        Mockito.when(mockToHitSix.getValue()).thenReturn(6);

        mockToHitEight = Mockito.mock(ToHitData.class);
        Mockito.when(mockToHitEight.getValue()).thenReturn(8);

        mockToHitThirteen = Mockito.mock(ToHitData.class);
        Mockito.when(mockToHitThirteen.getValue()).thenReturn(ToHitData.AUTOMATIC_FAIL);

        mockFireControl = Mockito.mock(FireControl.class);
        Mockito.when(mockFireControl.guessToHitModifierForWeapon(Mockito.any(Entity.class), Mockito.any(EntityState
                                                                                                                .class),
                                                                 Mockito.any(Targetable.class),
                                                                 Mockito.any(EntityState.class),
                                                                 Mockito.any(Mounted.class), Mockito.any(IGame.class)))
               .thenReturn(mockToHitEight);

        mockPrincess = Mockito.mock(Princess.class);
        Mockito.when(mockPrincess.getFireControl()).thenReturn(mockFireControl);

        mockShooter = Mockito.mock(BipedMech.class);
        Mockito.when(mockShooter.getPosition()).thenReturn(SHOOTER_COORDS);
        Mockito.when(mockShooter.getWeight()).thenReturn(75.0);
        Mockito.when(mockShooter.getId()).thenReturn(SHOOTER_ID);
        Mockito.when(mockShooter.getDisplayName()).thenReturn("Test shooter 1");
        Mockito.when(mockShooter.getEquipmentNum(Mockito.eq(mockWeapon))).thenReturn(WEAPON_ID);

        mockShooterState = Mockito.mock(EntityState.class);
        Mockito.when(mockShooterState.getPosition()).thenReturn(SHOOTER_COORDS);

        mockTarget = Mockito.mock(BipedMech.class);
        Mockito.when(mockTarget.getPosition()).thenReturn(TARGET_COORDS_9);
        Mockito.when(mockTarget.isLocationBad(Mockito.anyInt())).thenReturn(false);
        Mockito.when(mockTarget.getId()).thenReturn(TARGET_ID);
        Mockito.when(mockTarget.getTargetType()).thenReturn(Targetable.TYPE_ENTITY);
        Mockito.when(mockTarget.getDisplayName()).thenReturn("Mock target 2");

        mockTargetState = Mockito.mock(EntityState.class);
        Mockito.when(mockTargetState.getFacing()).thenReturn(TARGET_FACING);
        Mockito.when(mockTargetState.getPosition()).thenReturn(TARGET_COORDS_9);

        mockWeaponType = Mockito.mock(WeaponType.class);
        mockWeapon = Mockito.mock(Mounted.class);
        Mockito.when(mockWeapon.getType()).thenReturn(mockWeaponType);

        mockWeaponAttackAction = Mockito.mock(WeaponAttackAction.class);
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
        final double DELTA = 0.00001;

        WeaponFireInfo testWeaponFireInfo = Mockito.spy(new WeaponFireInfo(mockPrincess));
        testWeaponFireInfo.setShooter(mockShooter);
        testWeaponFireInfo.setShooterState(mockShooterState);
        testWeaponFireInfo.setTarget(mockTarget);
        testWeaponFireInfo.setTargetState(mockTargetState);
        testWeaponFireInfo.setWeapon(mockWeapon);
        testWeaponFireInfo.setGame(mockGame);

        // Test a medium laser vs light target with a to hit roll of 6.
        setupMediumLaser();
        setupLightTarget();
        double expectedMaxDamage = mockWeaponType.getDamage();
        double expectedProbabilityToHit = Compute.oddsAbove(mockToHitSix.getValue()) / 100;
        double expectedCriticals = 0.02460;
        double expectedKill = 0;
        Mockito.doReturn(mockToHitSix).when(testWeaponFireInfo).calcToHit();
        Mockito.doReturn(mockWeaponAttackAction).when(testWeaponFireInfo).buildWeaponAttackAction();
        Mockito.doReturn(expectedMaxDamage).when(testWeaponFireInfo).computeExpectedDamage();
        testWeaponFireInfo.initDamage(null, false, true);
        TestCase.assertEquals(expectedMaxDamage, testWeaponFireInfo.getMaxDamage());
        TestCase.assertEquals(expectedMaxDamage, testWeaponFireInfo.getExpectedDamageOnHit());
        TestCase.assertEquals(expectedProbabilityToHit, testWeaponFireInfo.getProbabilityToHit(), DELTA);
        TestCase.assertEquals(expectedCriticals, testWeaponFireInfo.getExpectedCriticals(), DELTA);
        TestCase.assertEquals(expectedKill, testWeaponFireInfo.getKillProbability(), DELTA);

        // Test a PPC vs light target with a to hit roll of 8.
        setupPPC();
        setupLightTarget();
        expectedMaxDamage = mockWeaponType.getDamage();
        expectedProbabilityToHit = Compute.oddsAbove(mockToHitEight.getValue()) / 100;
        expectedCriticals = 0.01867;
        expectedKill = 0.01155;
        Mockito.doReturn(mockToHitEight).when(testWeaponFireInfo).calcToHit();
        Mockito.doReturn(mockWeaponAttackAction).when(testWeaponFireInfo).buildWeaponAttackAction();
        Mockito.doReturn(expectedMaxDamage).when(testWeaponFireInfo).computeExpectedDamage();
        testWeaponFireInfo.initDamage(null, false, true);
        TestCase.assertEquals(expectedMaxDamage, testWeaponFireInfo.getMaxDamage());
        TestCase.assertEquals(expectedMaxDamage, testWeaponFireInfo.getExpectedDamageOnHit());
        TestCase.assertEquals(expectedProbabilityToHit, testWeaponFireInfo.getProbabilityToHit(), DELTA);
        TestCase.assertEquals(expectedCriticals, testWeaponFireInfo.getExpectedCriticals(), DELTA);
        TestCase.assertEquals(expectedKill, testWeaponFireInfo.getKillProbability(), DELTA);

        // Test a Gauss Rifle vs a light target with a to hit roll of 6.
        setupCGR();
        setupLightTarget();
        expectedMaxDamage = mockWeaponType.getDamage();
        expectedProbabilityToHit = Compute.oddsAbove(mockToHitSix.getValue()) / 100;
        expectedCriticals = 0.46129;
        expectedKill = 0.02005;
        Mockito.doReturn(mockToHitSix).when(testWeaponFireInfo).calcToHit();
        Mockito.doReturn(mockWeaponAttackAction).when(testWeaponFireInfo).buildWeaponAttackAction();
        Mockito.doReturn(expectedMaxDamage).when(testWeaponFireInfo).computeExpectedDamage();
        testWeaponFireInfo.initDamage(null, false, true);
        TestCase.assertEquals(expectedMaxDamage, testWeaponFireInfo.getMaxDamage());
        TestCase.assertEquals(expectedMaxDamage, testWeaponFireInfo.getExpectedDamageOnHit());
        TestCase.assertEquals(expectedProbabilityToHit, testWeaponFireInfo.getProbabilityToHit(), DELTA);
        TestCase.assertEquals(expectedCriticals, testWeaponFireInfo.getExpectedCriticals(), DELTA);
        TestCase.assertEquals(expectedKill, testWeaponFireInfo.getKillProbability(), DELTA);

        // Test a Gauss Rifle vs. a medium target with a to hit roll of 8.
        setupCGR();
        setupMediumTarget();
        expectedMaxDamage = mockWeaponType.getDamage();
        expectedProbabilityToHit = Compute.oddsAbove(mockToHitEight.getValue()) / 100;
        expectedCriticals = 0.01867;
        expectedKill = 0.01155;
        Mockito.doReturn(mockToHitEight).when(testWeaponFireInfo).calcToHit();
        Mockito.doReturn(mockWeaponAttackAction).when(testWeaponFireInfo).buildWeaponAttackAction();
        Mockito.doReturn(expectedMaxDamage).when(testWeaponFireInfo).computeExpectedDamage();
        testWeaponFireInfo.initDamage(null, false, true);
        TestCase.assertEquals(expectedMaxDamage, testWeaponFireInfo.getMaxDamage());
        TestCase.assertEquals(expectedMaxDamage, testWeaponFireInfo.getExpectedDamageOnHit());
        TestCase.assertEquals(expectedProbabilityToHit, testWeaponFireInfo.getProbabilityToHit(), DELTA);
        TestCase.assertEquals(expectedCriticals, testWeaponFireInfo.getExpectedCriticals(), DELTA);
        TestCase.assertEquals(expectedKill, testWeaponFireInfo.getKillProbability(), DELTA);

        // Test a medium laser vs. a medium target with no chance to hit.
        setupMediumLaser();
        setupMediumTarget();
        expectedMaxDamage = 0;
        expectedProbabilityToHit = Compute.oddsAbove(mockToHitThirteen.getValue()) / 100;
        expectedCriticals = 0;
        expectedKill = 0;
        Mockito.doReturn(mockToHitThirteen).when(testWeaponFireInfo).calcToHit();
        Mockito.doReturn(mockWeaponAttackAction).when(testWeaponFireInfo).buildWeaponAttackAction();
        Mockito.doReturn(expectedMaxDamage).when(testWeaponFireInfo).computeExpectedDamage();
        testWeaponFireInfo.initDamage(null, false, true);
        TestCase.assertEquals(expectedMaxDamage, testWeaponFireInfo.getMaxDamage());
        TestCase.assertEquals(expectedMaxDamage, testWeaponFireInfo.getExpectedDamageOnHit());
        TestCase.assertEquals(expectedProbabilityToHit, testWeaponFireInfo.getProbabilityToHit(), DELTA);
        TestCase.assertEquals(expectedCriticals, testWeaponFireInfo.getExpectedCriticals(), DELTA);
        TestCase.assertEquals(expectedKill, testWeaponFireInfo.getKillProbability(), DELTA);

        // todo build tests for AeroSpace attacks.
    }

}

/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.bot.princess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import megamek.client.bot.princess.FireControl.FireControlType;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentMode;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 12/18/13 1:02 PM
 */
class WeaponFireInfoTest {

    private static final int SHOOTER_ID = 1;
    private static final int TARGET_ID = 2;
    private static final int WEAPON_ID = 3;

    private static final Coords SHOOTER_COORDS = new Coords(10, 10);

    private static final Coords TARGET_COORDS_9 = new Coords(10, 19);
    private static final int TARGET_FACING = 3;

    private static ToHitData mockToHitEight;
    private static ToHitData mockToHitSix;
    private static ToHitData mockToHitThirteen;
    private static BipedMek mockShooter;
    private static EntityState mockShooterState;
    private static BipedMek mockTarget;
    private static EntityState mockTargetState;
    private static Game mockGame;
    private static WeaponMounted mockWeapon;
    private static WeaponType mockWeaponType;
    private static WeaponAttackAction mockWeaponAttackAction;
    private static Princess mockPrincess;

    static AmmoType mockArrowIVAmmoType = (AmmoType) EquipmentType.get("ISArrowIV Ammo");
    static AmmoType mockArrowIVHomingAmmoType = (AmmoType) EquipmentType.get("ISArrowIV Homing Ammo");

    @BeforeAll
    static void beforeAll() {
        mockGame = mock(Game.class);

        mockToHitSix = mock(ToHitData.class);
        when(mockToHitSix.getValue()).thenReturn(6);

        mockToHitEight = mock(ToHitData.class);
        when(mockToHitEight.getValue()).thenReturn(8);

        mockToHitThirteen = mock(ToHitData.class);
        when(mockToHitThirteen.getValue()).thenReturn(ToHitData.AUTOMATIC_FAIL);

        FireControl mockFireControl = mock(FireControl.class);
        when(mockFireControl.guessToHitModifierForWeapon(any(Entity.class), any(EntityState.class),
              any(Targetable.class), any(EntityState.class), any(WeaponMounted.class), any(AmmoMounted.class),
              any(Game.class)))
              .thenReturn(mockToHitEight);

        mockPrincess = mock(Princess.class);
        when(mockPrincess.getFireControl(FireControlType.Basic)).thenReturn(mockFireControl);
        when(mockPrincess.getMaxWeaponRange(any(Entity.class))).thenReturn(21);

        mockShooter = mock(BipedMek.class);
        when(mockShooter.getPosition()).thenReturn(SHOOTER_COORDS);
        when(mockShooter.getWeight()).thenReturn(75.0);
        when(mockShooter.getId()).thenReturn(SHOOTER_ID);
        when(mockShooter.getDisplayName()).thenReturn("Test shooter 1");
        when(mockShooter.getEquipmentNum(eq(mockWeapon))).thenReturn(WEAPON_ID);

        mockShooterState = mock(EntityState.class);
        when(mockShooterState.getPosition()).thenReturn(SHOOTER_COORDS);

        mockTarget = mock(BipedMek.class);
        when(mockTarget.getPosition()).thenReturn(TARGET_COORDS_9);
        when(mockTarget.isLocationBad(anyInt())).thenReturn(false);
        when(mockTarget.getId()).thenReturn(TARGET_ID);
        when(mockTarget.getTargetType()).thenReturn(Targetable.TYPE_ENTITY);
        when(mockTarget.getDisplayName()).thenReturn("Mock target 2");

        mockTargetState = mock(EntityState.class);
        when(mockTargetState.getFacing()).thenReturn(TARGET_FACING);
        when(mockTargetState.getPosition()).thenReturn(TARGET_COORDS_9);

        mockWeaponType = mock(WeaponType.class);
        mockWeapon = mock(WeaponMounted.class);
        EquipmentMode mockEquipmentMode = mock(EquipmentMode.class);
        when(mockWeapon.getType()).thenReturn(mockWeaponType);
        when(mockEquipmentMode.getName()).thenReturn("");
        when(mockWeapon.curMode()).thenReturn(mockEquipmentMode);

        mockWeaponAttackAction = mock(WeaponAttackAction.class);
        when(mockWeaponAttackAction.getEntity(any(Game.class))).thenReturn(mockShooter);
    }

    private void setupLightTarget() {
        when(mockTarget.getInternal(anyInt())).thenReturn(6);
        when(mockTarget.getInternal(eq(0))).thenReturn(3);
        when(mockTarget.getArmor(anyInt(), anyBoolean())).thenReturn(12);
        when(mockTarget.getArmor(eq(0), anyBoolean())).thenReturn(4);
    }

    private void setupMediumTarget() {
        when(mockTarget.getInternal(anyInt())).thenReturn(10);
        when(mockTarget.getInternal(eq(0))).thenReturn(3);
        when(mockTarget.getArmor(anyInt(), anyBoolean())).thenReturn(16);
        when(mockTarget.getArmor(eq(0), anyBoolean())).thenReturn(9);
    }

    private void setupMediumLaser() {
        when(mockWeaponType.getHeat()).thenReturn(3);
        when(mockWeaponType.getDamage()).thenReturn(5);
        when(mockWeaponType.getShortRange()).thenReturn(3);
        when(mockWeaponType.getMediumRange()).thenReturn(6);
        when(mockWeaponType.getLongRange()).thenReturn(9);
        when(mockWeapon.getDesc()).thenReturn("Medium Laser");
    }

    private void setupPPC() {
        when(mockWeaponType.getHeat()).thenReturn(10);
        when(mockWeaponType.getDamage()).thenReturn(10);
        when(mockWeaponType.getShortRange()).thenReturn(6);
        when(mockWeaponType.getMediumRange()).thenReturn(12);
        when(mockWeaponType.getLongRange()).thenReturn(18);
        when(mockWeapon.getDesc()).thenReturn("PPC");
    }

    private void setupCGR() {
        when(mockWeaponType.getHeat()).thenReturn(1);
        when(mockWeaponType.getDamage()).thenReturn(15);
        when(mockWeaponType.getShortRange()).thenReturn(7);
        when(mockWeaponType.getMediumRange()).thenReturn(15);
        when(mockWeaponType.getLongRange()).thenReturn(22);
        when(mockWeapon.getDesc()).thenReturn("Gauss Rifle (C)");
    }

    private void setupArrowIV() {
        when(mockWeaponType.getHeat()).thenReturn(10);
        when(mockWeaponType.getRackSize()).thenReturn(20);
        when(mockWeaponType.getShortRange()).thenReturn(1);
        when(mockWeaponType.getMediumRange()).thenReturn(2);
        when(mockWeaponType.getLongRange()).thenReturn(8);
        when(mockWeapon.getDesc()).thenReturn("IS Arrow IV Missile SystemFluff");
    }

    private WeaponFireInfo setupWFI() {
        WeaponFireInfo testWeaponFireInfo = spy(new WeaponFireInfo(mockPrincess));
        testWeaponFireInfo.setShooter(mockShooter);
        testWeaponFireInfo.setShooterState(mockShooterState);
        testWeaponFireInfo.setTarget(mockTarget);
        testWeaponFireInfo.setTargetState(mockTargetState);
        testWeaponFireInfo.setWeapon(mockWeapon);
        testWeaponFireInfo.setGame(mockGame);
        return testWeaponFireInfo;
    }

    @Test
    void testInitDamage() {
        final double DELTA = 0.00001;
        final double ROLL_TWO = 0.028;
        final double CRIT_COUNT = 0.611;

        WeaponFireInfo testWeaponFireInfo = setupWFI();

        // Test a medium laser vs light target with a to hit roll of 6.
        setupMediumLaser();
        setupLightTarget();
        double expectedMaxDamage = mockWeaponType.getDamage();
        double expectedProbabilityToHit = Compute.oddsAbove(mockToHitSix.getValue()) / 100;
        double expectedCriticalSlots = ROLL_TWO * CRIT_COUNT * expectedProbabilityToHit;
        double expectedKill = 0;
        doReturn(mockToHitSix).when(testWeaponFireInfo).calcToHit();
        doReturn(mockWeaponAttackAction).when(testWeaponFireInfo).buildWeaponAttackAction();
        doReturn(new double[] { expectedMaxDamage, 0D, 0D }).when(testWeaponFireInfo).computeExpectedDamage();
        when(mockShooter.getEquipment(anyInt())).thenReturn((Mounted) mockWeapon);
        testWeaponFireInfo.initDamage(null, false, true, null);
        assertEquals(expectedMaxDamage, testWeaponFireInfo.getMaxDamage());
        assertEquals(expectedProbabilityToHit, testWeaponFireInfo.getProbabilityToHit(), DELTA);
        assertEquals(expectedMaxDamage * expectedProbabilityToHit,
              testWeaponFireInfo.getExpectedDamage());
        assertEquals(expectedCriticalSlots, testWeaponFireInfo.getExpectedCriticals(), DELTA);
        assertEquals(expectedKill, testWeaponFireInfo.getKillProbability(), DELTA);

        // Test a PPC vs light target with a to hit roll of 8.
        setupPPC();
        testWeaponFireInfo = setupWFI();
        setupLightTarget();
        expectedMaxDamage = mockWeaponType.getDamage();
        expectedProbabilityToHit = Compute.oddsAbove(mockToHitEight.getValue()) / 100;
        expectedCriticalSlots = 0.0141773; // differs following first setup due to location destruction potential
        expectedKill = 0.0;
        doReturn(mockToHitEight).when(testWeaponFireInfo).calcToHit();
        doReturn(mockWeaponAttackAction).when(testWeaponFireInfo).buildWeaponAttackAction();
        doReturn(new double[] { expectedMaxDamage, 0D, 0D }).when(testWeaponFireInfo).computeExpectedDamage();
        testWeaponFireInfo.initDamage(null, false, true, null);
        assertEquals(expectedMaxDamage, testWeaponFireInfo.getMaxDamage());
        assertEquals(expectedMaxDamage * testWeaponFireInfo.getProbabilityToHit(),
              testWeaponFireInfo.getExpectedDamage());
        assertEquals(expectedProbabilityToHit, testWeaponFireInfo.getProbabilityToHit(), DELTA);
        assertEquals(expectedCriticalSlots, testWeaponFireInfo.getExpectedCriticals(), DELTA);
        assertEquals(expectedKill, testWeaponFireInfo.getKillProbability(), DELTA);

        // Test a Gauss Rifle vs a light target with a to hit roll of 6.
        setupCGR();
        testWeaponFireInfo = setupWFI();
        setupLightTarget();
        expectedMaxDamage = mockWeaponType.getDamage();
        expectedProbabilityToHit = Compute.oddsAbove(mockToHitSix.getValue()) / 100;
        expectedCriticalSlots = 0.0324; // differs following first setup due to location destruction potential
        expectedKill = 0.02005;
        doReturn(mockToHitSix).when(testWeaponFireInfo).calcToHit();
        doReturn(mockWeaponAttackAction).when(testWeaponFireInfo).buildWeaponAttackAction();
        doReturn(new double[] { expectedMaxDamage, 0D, 0D }).when(testWeaponFireInfo).computeExpectedDamage();
        testWeaponFireInfo.initDamage(null, false, true, null);
        assertEquals(expectedMaxDamage, testWeaponFireInfo.getMaxDamage());
        assertEquals(expectedMaxDamage * testWeaponFireInfo.getProbabilityToHit(),
              testWeaponFireInfo.getExpectedDamage());
        assertEquals(expectedProbabilityToHit, testWeaponFireInfo.getProbabilityToHit(), DELTA);
        assertEquals(expectedCriticalSlots, testWeaponFireInfo.getExpectedCriticals(), DELTA);
        assertEquals(expectedKill, testWeaponFireInfo.getKillProbability(), DELTA);

        // Test a Gauss Rifle vs. a medium target with a to hit roll of 8.
        setupCGR();
        testWeaponFireInfo = setupWFI();
        setupMediumTarget();
        expectedMaxDamage = mockWeaponType.getDamage();
        expectedProbabilityToHit = Compute.oddsAbove(mockToHitEight.getValue()) / 100;
        expectedCriticalSlots = ROLL_TWO * CRIT_COUNT * expectedProbabilityToHit;
        expectedKill = 0.0;
        doReturn(mockToHitEight).when(testWeaponFireInfo).calcToHit();
        doReturn(mockWeaponAttackAction).when(testWeaponFireInfo).buildWeaponAttackAction();
        doReturn(new double[] { expectedMaxDamage, 0D, 0D }).when(testWeaponFireInfo).computeExpectedDamage();
        testWeaponFireInfo.initDamage(null, false, true, null);
        assertEquals(expectedMaxDamage, testWeaponFireInfo.getMaxDamage());
        assertEquals(expectedMaxDamage * testWeaponFireInfo.getProbabilityToHit(),
              testWeaponFireInfo.getExpectedDamage());
        assertEquals(expectedProbabilityToHit, testWeaponFireInfo.getProbabilityToHit(), DELTA);
        assertEquals(expectedCriticalSlots, testWeaponFireInfo.getExpectedCriticals(), DELTA);
        assertEquals(expectedKill, testWeaponFireInfo.getKillProbability(), DELTA);

        // Test a medium laser vs. a medium target with no chance to hit.
        setupMediumLaser();
        testWeaponFireInfo = setupWFI();
        setupMediumTarget();
        expectedMaxDamage = 0;
        expectedProbabilityToHit = Compute.oddsAbove(mockToHitThirteen.getValue()) / 100;
        expectedCriticalSlots = ROLL_TWO * CRIT_COUNT * expectedProbabilityToHit;
        expectedKill = 0;
        doReturn(mockToHitThirteen).when(testWeaponFireInfo).calcToHit();
        doReturn(mockWeaponAttackAction).when(testWeaponFireInfo).buildWeaponAttackAction();
        doReturn(new double[] { expectedMaxDamage, 0D, 0D }).when(testWeaponFireInfo).computeExpectedDamage();
        testWeaponFireInfo.initDamage(null, false, true, null);
        assertEquals(expectedMaxDamage, testWeaponFireInfo.getMaxDamage());
        assertEquals(expectedMaxDamage, testWeaponFireInfo.getDamageOnHit());
        assertEquals(expectedProbabilityToHit, testWeaponFireInfo.getProbabilityToHit(), DELTA);
        assertEquals(expectedCriticalSlots, testWeaponFireInfo.getExpectedCriticals(), DELTA);
        assertEquals(expectedKill, testWeaponFireInfo.getKillProbability(), DELTA);

        // todo build tests for AeroSpace attacks.
    }

    @Test
    void testPostProcessingNoHomingNoTAGAround() {
        // Should be no-op if conditions not met
        WeaponFireInfo testWeaponFireInfo = setupWFI();
        setupArrowIV();
        setupMediumTarget();

        // Configure ammo
        AmmoMounted ammo = new AmmoMounted(mockShooter, mockArrowIVAmmoType);
        testWeaponFireInfo.setAmmo(ammo);

        // Set up THDs
        ToHitData originalTHD = new ToHitData(4, "Gunnery SKill");
        originalTHD.addModifier(1, "Walked");
        originalTHD.addModifier(2, "Target Moved Some");
        originalTHD.addModifier(1, "Intervening shrubbery");

        // Expect to get the original THD back if no processing is needed
        ToHitData processed = testWeaponFireInfo.postProcessToHit(originalTHD);
        assertEquals(originalTHD.getValue(), processed.getValue());
        assertEquals(originalTHD.getCumulativePlainDesc(), processed.getCumulativePlainDesc());
        assertEquals(originalTHD, processed);
    }

    @Test
    void testPostProcessingHomingNoTAGAround() {
        // Should return impossible THD if Homing shot but no TAG friends are around
        WeaponFireInfo testWeaponFireInfo = setupWFI();
        setupArrowIV();
        setupMediumTarget();

        // Configure ammo
        AmmoMounted ammo = new AmmoMounted(mockShooter, mockArrowIVAmmoType);
        ammo.changeAmmoType(mockArrowIVHomingAmmoType);
        ammo.setMode("Homing");
        testWeaponFireInfo.setAmmo(ammo);

        // Set up THDs
        String msg = "Blah Blah need 4+ whatever";
        ToHitData originalTHD = new ToHitData(4, msg);
        ToHitData expectedTHD = new ToHitData(ToHitData.AUTOMATIC_FAIL, msg);

        // Expect to get a new THD back if processing is needed
        ToHitData processed = testWeaponFireInfo.postProcessToHit(originalTHD);
        assertEquals(expectedTHD.getValue(), processed.getValue());
        assertEquals(expectedTHD.getDesc(), processed.getDesc());
        assertNotEquals(originalTHD, processed);
    }
}

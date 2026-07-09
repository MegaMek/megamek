/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.common.compute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.weapons.Weapon;
import megamek.common.weapons.autoCannons.RACWeapon;
import megamek.common.weapons.autoCannons.UACWeapon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Compute#spinUpCannon(Game, WeaponAttackAction, int)}.
 *
 * <p>Besides the spin-up mode ladder, these tests pin down the performance contract that motivated the
 * method's guard ordering: the weapon-type check must run BEFORE any to-hit computation, because the
 * method is invoked for every weapon the bot evaluates while ranking candidate move paths and the full
 * to-hit calculation is very expensive. A non-autocannon must return without a single {@code toHit} call,
 * and autocannons must pay for exactly one.</p>
 */
class ComputeSpinUpCannonTest {

    private static final int WEAPON_ID = 3;
    private static final int SPIN_UP_THRESHOLD = 7;

    private Game mockGame;
    private WeaponAttackAction mockAttackAction;
    private Entity mockShooter;
    private Mounted<?> mockWeapon;

    @BeforeEach
    void beforeEach() {
        mockGame = mock(Game.class);
        GameOptions mockOptions = mock(GameOptions.class);
        when(mockGame.getOptions()).thenReturn(mockOptions);

        mockShooter = mock(Mek.class);
        mockWeapon = mock(Mounted.class);

        mockAttackAction = mock(WeaponAttackAction.class);
        when(mockAttackAction.getEntity(mockGame)).thenReturn(mockShooter);
        when(mockAttackAction.getWeaponId()).thenReturn(WEAPON_ID);
        // The cast in spinUpCannon uses the raw Mounted, so stub the same accessor it calls.
        when(mockShooter.getEquipment(WEAPON_ID)).thenAnswer(invocation -> mockWeapon);
    }

    private void arrangeWeaponType(WeaponType weaponType) {
        when(mockWeapon.getType()).thenAnswer(invocation -> weaponType);
    }

    private void arrangeToHitValue(int toHitValue) {
        when(mockAttackAction.toHit(mockGame)).thenReturn(new ToHitData(toHitValue, "test"));
    }

    @Test
    void testNonAutocannonReturnsZeroWithoutComputingToHit() {
        // A plain (non-AC) weapon type: not an ACWeapon, UACWeapon or RACWeapon
        arrangeWeaponType(mock(WeaponType.class));

        int spinMode = Compute.spinUpCannon(mockGame, mockAttackAction, SPIN_UP_THRESHOLD);

        assertEquals(0, spinMode);
        // The performance contract: no to-hit computation and no mode fiddling for non-autocannons.
        verify(mockAttackAction, never()).toHit(any(Game.class));
        verify(mockWeapon, never()).setMode(any(String.class));
        verify(mockWeapon, never()).setMode(anyInt());
    }

    @Test
    void testNullArgumentsReturnZero() {
        assertEquals(0, Compute.spinUpCannon(null, mockAttackAction, SPIN_UP_THRESHOLD));
        assertEquals(0, Compute.spinUpCannon(mockGame, null, SPIN_UP_THRESHOLD));
    }

    @Test
    void testInvalidWeaponIdReturnsZeroWithoutException() {
        // An out-of-range weapon id makes getEquipment return null; the method must guard it, not NPE.
        when(mockShooter.getEquipment(WEAPON_ID)).thenReturn(null);

        assertEquals(0, Compute.spinUpCannon(mockGame, mockAttackAction, SPIN_UP_THRESHOLD));
    }

    @Test
    void testUltraAutocannonAtThresholdSpinsUpWithSingleToHitComputation() {
        arrangeWeaponType(mock(UACWeapon.class));
        arrangeToHitValue(SPIN_UP_THRESHOLD);

        int spinMode = Compute.spinUpCannon(mockGame, mockAttackAction, SPIN_UP_THRESHOLD);

        assertEquals(1, spinMode);
        verify(mockWeapon).setMode(Weapon.MODE_UAC_ULTRA);
        // The other performance contract: the expensive to-hit number is computed exactly once.
        verify(mockAttackAction, times(1)).toHit(mockGame);
    }

    @Test
    void testUltraAutocannonAboveThresholdStaysSingleShot() {
        arrangeWeaponType(mock(UACWeapon.class));
        arrangeToHitValue(SPIN_UP_THRESHOLD + 1);

        int spinMode = Compute.spinUpCannon(mockGame, mockAttackAction, SPIN_UP_THRESHOLD);

        assertEquals(0, spinMode);
        verify(mockWeapon).setMode(Weapon.MODE_AC_SINGLE);
        verify(mockAttackAction, times(1)).toHit(mockGame);
    }

    @Test
    void testAutocannonThatCannotHitReturnsZero() {
        arrangeWeaponType(mock(UACWeapon.class));
        arrangeToHitValue(13);

        int spinMode = Compute.spinUpCannon(mockGame, mockAttackAction, SPIN_UP_THRESHOLD);

        assertEquals(0, spinMode);
        verify(mockWeapon, never()).setMode(any(String.class));
    }

    @Test
    void testRotaryAutocannonAtThresholdSetsTwoShot() {
        arrangeWeaponType(mock(RACWeapon.class));
        arrangeToHitValue(SPIN_UP_THRESHOLD);

        int spinMode = Compute.spinUpCannon(mockGame, mockAttackAction, SPIN_UP_THRESHOLD);

        assertEquals(1, spinMode);
        verify(mockWeapon).setMode(Weapon.MODE_RAC_TWO_SHOT);
        verify(mockAttackAction, times(1)).toHit(mockGame);
    }

    @Test
    void testRotaryAutocannonWellUnderThresholdSetsSixShot() {
        arrangeWeaponType(mock(RACWeapon.class));
        arrangeToHitValue(SPIN_UP_THRESHOLD - 3);

        int spinMode = Compute.spinUpCannon(mockGame, mockAttackAction, SPIN_UP_THRESHOLD);

        assertEquals(5, spinMode);
        verify(mockWeapon).setMode(Weapon.MODE_RAC_SIX_SHOT);
    }

    @Test
    void testRotaryAutocannonTwoUnderThresholdSetsFiveShot() {
        arrangeWeaponType(mock(RACWeapon.class));
        arrangeToHitValue(SPIN_UP_THRESHOLD - 2);

        int spinMode = Compute.spinUpCannon(mockGame, mockAttackAction, SPIN_UP_THRESHOLD);

        assertEquals(4, spinMode);
        verify(mockWeapon).setMode(Weapon.MODE_RAC_FIVE_SHOT);
    }

    @Test
    void testRotaryAutocannonOneUnderThresholdSetsThreeShotForHighToHit() {
        arrangeWeaponType(mock(RACWeapon.class));
        // threshold 7 - 1 = to-hit 6, which is >= 6, so the conservative three-shot mode applies
        arrangeToHitValue(SPIN_UP_THRESHOLD - 1);

        int spinMode = Compute.spinUpCannon(mockGame, mockAttackAction, SPIN_UP_THRESHOLD);

        assertEquals(2, spinMode);
        verify(mockWeapon).setMode(Weapon.MODE_RAC_THREE_SHOT);
    }

    @Test
    void testRotaryAutocannonOneUnderThresholdSetsFourShotForLowToHit() {
        arrangeWeaponType(mock(RACWeapon.class));
        // threshold 5 - 1 = to-hit 4, which is < 6, so the more aggressive four-shot mode applies
        int lowThreshold = 5;
        arrangeToHitValue(lowThreshold - 1);

        int spinMode = Compute.spinUpCannon(mockGame, mockAttackAction, lowThreshold);

        assertEquals(3, spinMode);
        verify(mockWeapon).setMode(Weapon.MODE_RAC_FOUR_SHOT);
    }
}

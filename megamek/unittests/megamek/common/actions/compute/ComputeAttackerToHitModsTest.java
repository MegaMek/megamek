/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.actions.compute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.EnumSet;

import megamek.common.LosEffects;
import megamek.common.ToHitData;
import megamek.common.compute.Compute;
import megamek.common.enums.AimingMode;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.units.Entity;
import megamek.common.units.LandAirMek;
import megamek.common.units.QuadVee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Tests for {@link ComputeAttackerToHitMods#compileAttackerToHitMods}.
 * <p>
 * This class contains tests for various attacker to-hit modifiers including mode conversion
 * penalties for LAMs and QuadVees.
 */
class ComputeAttackerToHitModsTest {

    private Game mockGame;
    private GameOptions mockOptions;
    private LosEffects mockLos;
    private WeaponMounted mockWeapon;
    private WeaponType mockWeaponType;

    @BeforeEach
    void setUp() {
        // Mock game options
        mockOptions = mock(GameOptions.class);
        when(mockOptions.booleanOption(anyString())).thenReturn(false);

        // Mock game
        mockGame = mock(Game.class);
        when(mockGame.getOptions()).thenReturn(mockOptions);

        // Mock LOS
        mockLos = mock(LosEffects.class);

        // Mock weapon type
        mockWeaponType = mock(WeaponType.class);
        when(mockWeaponType.getName()).thenReturn("Test Weapon");

        // Mock weapon
        mockWeapon = mock(WeaponMounted.class);
        when(mockWeapon.getType()).thenReturn(mockWeaponType);
        when(mockWeapon.getLocation()).thenReturn(0);
    }

    private ToHitData callCompileAttackerToHitMods(Entity attacker) {
        try (MockedStatic<Compute> mockedCompute = mockStatic(Compute.class)) {
            // Mock static Compute methods to return empty ToHitData
            mockedCompute.when(() -> Compute.getAttackerMovementModifier(any(Game.class), anyInt()))
                  .thenReturn(new ToHitData());
            mockedCompute.when(() -> Compute.getProneMods(any(Game.class), any(Entity.class), anyInt()))
                  .thenReturn(new ToHitData());
            mockedCompute.when(() -> Compute.getSecondaryTargetMod(any(Game.class), any(Entity.class), any()))
                  .thenReturn(new ToHitData());

            ToHitData toHit = new ToHitData();
            return ComputeAttackerToHitMods.compileAttackerToHitMods(
                  mockGame,
                  attacker,
                  null, // target
                  mockLos,
                  toHit,
                  -1, // aimingAt
                  AimingMode.NONE,
                  mockWeaponType,
                  mockWeapon,
                  1, // weaponId
                  null, // ammoType
                  EnumSet.noneOf(AmmoType.Munitions.class),
                  false, // isFlakAttack
                  false, // isHaywireINarced
                  false, // isNemesisConfused
                  false, // isWeaponFieldGuns
                  false  // usesAmmo
            );
        }
    }

    /**
     * Tests for LAM mode conversion to-hit penalty.
     * <p>
     * Per IO:AE p.101, LAMs suffer +3 to-hit when converting between modes.
     */
    @Nested
    @DisplayName("LAM Conversion Penalty Tests")
    class LamConversionTests {

        @Test
        @DisplayName("LAM converting modes gets +3 to-hit penalty relative to not converting")
        void lamConverting_getsPlusThreeModifierOverBaseline() {
            // Create LAM mock that is NOT converting
            LandAirMek mockLamNotConverting = mock(LandAirMek.class);
            when(mockLamNotConverting.getId()).thenReturn(1);
            when(mockLamNotConverting.isConvertingNow()).thenReturn(false);
            when(mockLamNotConverting.isAirborne()).thenReturn(false);
            when(mockLamNotConverting.getPosition()).thenReturn(null);

            // Create LAM mock that IS converting
            LandAirMek mockLamConverting = mock(LandAirMek.class);
            when(mockLamConverting.getId()).thenReturn(1);
            when(mockLamConverting.isConvertingNow()).thenReturn(true);
            when(mockLamConverting.isAirborne()).thenReturn(false);
            when(mockLamConverting.getPosition()).thenReturn(null);

            ToHitData notConvertingResult = callCompileAttackerToHitMods(mockLamNotConverting);
            ToHitData convertingResult = callCompileAttackerToHitMods(mockLamConverting);

            int difference = convertingResult.getValue() - notConvertingResult.getValue();
            assertEquals(3, difference,
                  "Converting LAM should have +3 modifier compared to non-converting LAM");
            assertTrue(convertingResult.getDesc().contains("converting"),
                  "Should contain 'converting' in description");
        }
    }

    /**
     * Tests for QuadVee mode conversion to-hit penalty.
     * <p>
     * Per TM, QuadVees suffer +3 to-hit when converting between modes.
     */
    @Nested
    @DisplayName("QuadVee Conversion Penalty Tests")
    class QuadVeeConversionTests {

        @Test
        @DisplayName("QuadVee converting modes gets +3 to-hit penalty relative to not converting")
        void quadVeeConverting_getsPlusThreeModifierOverBaseline() {
            // Create QuadVee mock that is NOT converting
            QuadVee mockQuadVeeNotConverting = mock(QuadVee.class);
            when(mockQuadVeeNotConverting.getId()).thenReturn(1);
            when(mockQuadVeeNotConverting.isConvertingNow()).thenReturn(false);
            when(mockQuadVeeNotConverting.isAirborne()).thenReturn(false);
            when(mockQuadVeeNotConverting.getPosition()).thenReturn(null);

            // Create QuadVee mock that IS converting
            QuadVee mockQuadVeeConverting = mock(QuadVee.class);
            when(mockQuadVeeConverting.getId()).thenReturn(1);
            when(mockQuadVeeConverting.isConvertingNow()).thenReturn(true);
            when(mockQuadVeeConverting.isAirborne()).thenReturn(false);
            when(mockQuadVeeConverting.getPosition()).thenReturn(null);

            ToHitData notConvertingResult = callCompileAttackerToHitMods(mockQuadVeeNotConverting);
            ToHitData convertingResult = callCompileAttackerToHitMods(mockQuadVeeConverting);

            int difference = convertingResult.getValue() - notConvertingResult.getValue();
            assertEquals(3, difference,
                  "Converting QuadVee should have +3 modifier compared to non-converting QuadVee");
            assertTrue(convertingResult.getDesc().contains("converting"),
                  "Should contain 'converting' in description");
        }
    }
}

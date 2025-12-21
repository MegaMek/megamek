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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.ToHitData;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Crew;
import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for Prototype Direct Neural Interface (Proto DNI) to-hit modifiers. Per IO pg 83, Proto DNI provides -2 gunnery
 * modifier. Proto DNI takes precedence over VDNI/BVDNI and doesn't stack with them.
 *
 * @see ComputeAttackerToHitMods#compileCrewToHitMods
 */
class PrototypeDniToHitModsTest {

    private Game mockGame;
    private GameOptions mockOptions;
    private Crew mockCrew;
    private Entity mockEntity;
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

        // Mock crew
        mockCrew = mock(Crew.class);
        when(mockCrew.getHits()).thenReturn(0);

        // Mock entity (non-infantry by default)
        mockEntity = mock(Entity.class);
        when(mockEntity.isConventionalInfantry()).thenReturn(false);
        when(mockEntity.getCrew()).thenReturn(mockCrew);

        // Mock weapon type
        mockWeaponType = mock(WeaponType.class);
        when(mockWeaponType.getName()).thenReturn("Test Weapon");

        // Mock weapon
        mockWeapon = mock(WeaponMounted.class);
        when(mockWeapon.getType()).thenReturn(mockWeaponType);
    }

    @Nested
    @DisplayName("Basic Proto DNI Modifier Tests")
    class BasicProtoDniTests {

        @Test
        @DisplayName("Entity with Proto DNI gets -2 gunnery modifier")
        void entityWithProtoDni_getsMinusTwoModifier() {
            when(mockEntity.hasAbility(anyString())).thenAnswer(invocation -> {
                String arg = invocation.getArgument(0);
                return OptionsConstants.MD_PROTO_DNI.equals(arg);
            });

            ToHitData toHit = new ToHitData();
            toHit = ComputeAttackerToHitMods.compileCrewToHitMods(mockGame, mockEntity, toHit, mockWeapon);

            assertEquals(-2, toHit.getValue());
            assertTrue(toHit.getDesc().contains("Prototype DNI"),
                  "Should show Prototype DNI message");
        }

        @Test
        @DisplayName("Entity without Proto DNI gets no modifier from it")
        void entityWithoutProtoDni_getsNoModifier() {
            when(mockEntity.hasAbility(anyString())).thenReturn(false);

            ToHitData toHit = new ToHitData();
            toHit = ComputeAttackerToHitMods.compileCrewToHitMods(mockGame, mockEntity, toHit, mockWeapon);

            assertEquals(0, toHit.getValue());
        }
    }

    @Nested
    @DisplayName("Proto DNI Precedence Tests")
    class PrecedenceTests {

        @Test
        @DisplayName("Proto DNI takes precedence over VDNI (gets -2, not -3)")
        void protoDniWithVdni_getsMinusTwoOnly() {
            when(mockEntity.hasAbility(anyString())).thenAnswer(invocation -> {
                String arg = invocation.getArgument(0);
                return OptionsConstants.MD_PROTO_DNI.equals(arg)
                      || OptionsConstants.MD_VDNI.equals(arg);
            });

            ToHitData toHit = new ToHitData();
            toHit = ComputeAttackerToHitMods.compileCrewToHitMods(mockGame, mockEntity, toHit, mockWeapon);

            assertEquals(-2, toHit.getValue(),
                  "Proto DNI (-2) should take precedence over VDNI (-1), not stack");
            assertTrue(toHit.getDesc().contains("Prototype DNI"),
                  "Should show Prototype DNI message, not VDNI");
        }

        @Test
        @DisplayName("Proto DNI takes precedence over BVDNI (gets -2, not -3)")
        void protoDniWithBvdni_getsMinusTwoOnly() {
            when(mockEntity.hasAbility(anyString())).thenAnswer(invocation -> {
                String arg = invocation.getArgument(0);
                return OptionsConstants.MD_PROTO_DNI.equals(arg)
                      || OptionsConstants.MD_BVDNI.equals(arg);
            });

            ToHitData toHit = new ToHitData();
            toHit = ComputeAttackerToHitMods.compileCrewToHitMods(mockGame, mockEntity, toHit, mockWeapon);

            assertEquals(-2, toHit.getValue(),
                  "Proto DNI (-2) should take precedence over BVDNI (-1), not stack");
            assertTrue(toHit.getDesc().contains("Prototype DNI"),
                  "Should show Prototype DNI message, not BVDNI");
        }

        @Test
        @DisplayName("Proto DNI with both VDNI and BVDNI still gets only -2")
        void protoDniWithVdniAndBvdni_getsMinusTwoOnly() {
            when(mockEntity.hasAbility(anyString())).thenAnswer(invocation -> {
                String arg = invocation.getArgument(0);
                return OptionsConstants.MD_PROTO_DNI.equals(arg)
                      || OptionsConstants.MD_VDNI.equals(arg)
                      || OptionsConstants.MD_BVDNI.equals(arg);
            });

            ToHitData toHit = new ToHitData();
            toHit = ComputeAttackerToHitMods.compileCrewToHitMods(mockGame, mockEntity, toHit, mockWeapon);

            assertEquals(-2, toHit.getValue(),
                  "Proto DNI should take precedence, no stacking with VDNI/BVDNI");
        }
    }

    @Nested
    @DisplayName("Proto DNI with Multi-Modal Implants Tests")
    class MultiModalImplantsSynergyTests {

        @Test
        @DisplayName("Proto DNI enables MM implant synergy for non-infantry")
        void protoDniWithMmImplants_getsCombinedBonus() {
            when(mockEntity.hasAbility(anyString())).thenAnswer(invocation -> {
                String arg = invocation.getArgument(0);
                return OptionsConstants.MD_PROTO_DNI.equals(arg)
                      || OptionsConstants.MD_MM_IMPLANTS.equals(arg);
            });

            ToHitData toHit = new ToHitData();
            toHit = ComputeAttackerToHitMods.compileCrewToHitMods(mockGame, mockEntity, toHit, mockWeapon);

            assertEquals(-3, toHit.getValue(),
                  "Proto DNI (-2) + MM implants synced via DNI (-1) = -3 total");
        }

        @Test
        @DisplayName("Proto DNI enables Enhanced MM implant synergy for non-infantry")
        void protoDniWithEnhMmImplants_getsCombinedBonus() {
            when(mockEntity.hasAbility(anyString())).thenAnswer(invocation -> {
                String arg = invocation.getArgument(0);
                return OptionsConstants.MD_PROTO_DNI.equals(arg)
                      || OptionsConstants.MD_ENH_MM_IMPLANTS.equals(arg);
            });

            ToHitData toHit = new ToHitData();
            toHit = ComputeAttackerToHitMods.compileCrewToHitMods(mockGame, mockEntity, toHit, mockWeapon);

            assertEquals(-3, toHit.getValue(),
                  "Proto DNI (-2) + Enhanced MM implants synced via DNI (-1) = -3 total");
        }

        @Test
        @DisplayName("Proto DNI + MM implants don't double-stack with VDNI")
        void protoDniWithMmImplantsAndVdni_getsMinusThreeOnly() {
            when(mockEntity.hasAbility(anyString())).thenAnswer(invocation -> {
                String arg = invocation.getArgument(0);
                return OptionsConstants.MD_PROTO_DNI.equals(arg)
                      || OptionsConstants.MD_VDNI.equals(arg)
                      || OptionsConstants.MD_MM_IMPLANTS.equals(arg);
            });

            ToHitData toHit = new ToHitData();
            toHit = ComputeAttackerToHitMods.compileCrewToHitMods(mockGame, mockEntity, toHit, mockWeapon);

            assertEquals(-3, toHit.getValue(),
                  "Proto DNI (-2) + MM implants (-1) = -3, VDNI doesn't add more");
        }
    }

    @Nested
    @DisplayName("Comparison with VDNI/BVDNI Tests")
    class ComparisonTests {

        @Test
        @DisplayName("VDNI alone gives -1 (for comparison)")
        void vdniAlone_getsMinusOne() {
            when(mockEntity.hasAbility(anyString())).thenAnswer(invocation -> {
                String arg = invocation.getArgument(0);
                return OptionsConstants.MD_VDNI.equals(arg);
            });

            ToHitData toHit = new ToHitData();
            toHit = ComputeAttackerToHitMods.compileCrewToHitMods(mockGame, mockEntity, toHit, mockWeapon);

            assertEquals(-1, toHit.getValue(),
                  "VDNI alone should give -1 gunnery");
        }

        @Test
        @DisplayName("BVDNI alone gives -1 (for comparison)")
        void bvdniAlone_getsMinusOne() {
            when(mockEntity.hasAbility(anyString())).thenAnswer(invocation -> {
                String arg = invocation.getArgument(0);
                return OptionsConstants.MD_BVDNI.equals(arg);
            });

            ToHitData toHit = new ToHitData();
            toHit = ComputeAttackerToHitMods.compileCrewToHitMods(mockGame, mockEntity, toHit, mockWeapon);

            assertEquals(-1, toHit.getValue(),
                  "BVDNI alone should give -1 gunnery");
        }
    }
}

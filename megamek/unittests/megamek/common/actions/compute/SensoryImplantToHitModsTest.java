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
 * Tests for Cybernetic Sensory Implant to-hit modifiers. Per IO pg 78, laser-sight and telescopic optical implants
 * provide -1 to-hit for infantry. Benefits within the same category don't stack.
 *
 * @see ComputeAttackerToHitMods#compileCrewToHitMods
 */
class SensoryImplantToHitModsTest {

    private Game mockGame;
    private GameOptions mockOptions;
    private Crew mockCrew;
    private Entity mockInfantry;
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

        // Mock infantry entity
        mockInfantry = mock(Entity.class);
        when(mockInfantry.isConventionalInfantry()).thenReturn(true);
        when(mockInfantry.getCrew()).thenReturn(mockCrew);

        // Mock weapon type
        mockWeaponType = mock(WeaponType.class);
        when(mockWeaponType.getName()).thenReturn("Test Weapon");

        // Mock weapon
        mockWeapon = mock(WeaponMounted.class);
        when(mockWeapon.getType()).thenReturn(mockWeaponType);
    }

    @Nested
    @DisplayName("Laser-Sight Optical Implant Tests")
    class LaserSightImplantTests {

        @Test
        @DisplayName("Infantry with laser-sight implant gets -1 to-hit")
        void infantryWithLaserImplant_getsMinusOneModifier() {
            when(mockInfantry.hasAbility(anyString())).thenAnswer(invocation -> {
                String arg = invocation.getArgument(0);
                return OptionsConstants.MD_CYBER_IMP_LASER.equals(arg);
            });

            ToHitData toHit = new ToHitData();
            toHit = ComputeAttackerToHitMods.compileCrewToHitMods(mockGame, mockInfantry, toHit, mockWeapon);

            assertEquals(-1, toHit.getValue());
            assertTrue(toHit.getDesc().contains("MD laser-sighting"),
                  "Should show laser-sighting message");
        }
    }

    @Nested
    @DisplayName("Telescopic Optical Implant Tests")
    class TelescopicImplantTests {

        @Test
        @DisplayName("Infantry with telescopic implant gets -1 to-hit")
        void infantryWithTeleImplant_getsMinusOneModifier() {
            when(mockInfantry.hasAbility(anyString())).thenAnswer(invocation -> {
                String arg = invocation.getArgument(0);
                return OptionsConstants.MD_CYBER_IMP_TELE.equals(arg);
            });

            ToHitData toHit = new ToHitData();
            toHit = ComputeAttackerToHitMods.compileCrewToHitMods(mockGame, mockInfantry, toHit, mockWeapon);

            assertEquals(-1, toHit.getValue());
            assertTrue(toHit.getDesc().contains("MD telescopic optics"),
                  "Should show telescopic optics message");
        }
    }

    @Nested
    @DisplayName("Combined Implants Tests")
    class CombinedImplantsTests {

        @Test
        @DisplayName("Infantry with both implants gets only -1 to-hit (no stacking)")
        void infantryWithBothImplants_getsOnlyMinusOne() {
            when(mockInfantry.hasAbility(anyString())).thenAnswer(invocation -> {
                String arg = invocation.getArgument(0);
                return OptionsConstants.MD_CYBER_IMP_LASER.equals(arg)
                      || OptionsConstants.MD_CYBER_IMP_TELE.equals(arg);
            });

            ToHitData toHit = new ToHitData();
            toHit = ComputeAttackerToHitMods.compileCrewToHitMods(mockGame, mockInfantry, toHit, mockWeapon);

            assertEquals(-1, toHit.getValue(), "Benefits should not stack - still only -1");
            assertTrue(toHit.getDesc().contains("MD targeting implants"),
                  "Should show combined targeting implants message");
        }
    }

    @Nested
    @DisplayName("Non-Infantry Entity Tests")
    class NonInfantryTests {

        @Test
        @DisplayName("Non-infantry with laser implant gets no modifier")
        void nonInfantryWithLaserImplant_getsNoModifier() {
            Entity mockMek = mock(Entity.class);
            when(mockMek.isConventionalInfantry()).thenReturn(false);
            when(mockMek.getCrew()).thenReturn(mockCrew);
            when(mockMek.hasAbility(anyString())).thenAnswer(invocation -> {
                String arg = invocation.getArgument(0);
                return OptionsConstants.MD_CYBER_IMP_LASER.equals(arg);
            });

            ToHitData toHit = new ToHitData();
            toHit = ComputeAttackerToHitMods.compileCrewToHitMods(mockGame, mockMek, toHit, mockWeapon);

            assertEquals(0, toHit.getValue(), "Non-infantry should not get sensory implant bonus");
        }
    }

    @Nested
    @DisplayName("No Implants Tests")
    class NoImplantsTests {

        @Test
        @DisplayName("Infantry without implants gets no modifier")
        void infantryWithoutImplants_getsNoModifier() {
            when(mockInfantry.hasAbility(anyString())).thenReturn(false);

            ToHitData toHit = new ToHitData();
            toHit = ComputeAttackerToHitMods.compileCrewToHitMods(mockGame, mockInfantry, toHit, mockWeapon);

            assertEquals(0, toHit.getValue());
        }
    }

    @Nested
    @DisplayName("Probe Implants (Audio/Visual) Tests")
    class ProbeImplantsTests {

        @Test
        @DisplayName("Audio implant does not provide to-hit bonus")
        void audioImplant_noToHitBonus() {
            when(mockInfantry.hasAbility(anyString())).thenAnswer(invocation -> {
                String arg = invocation.getArgument(0);
                return OptionsConstants.MD_CYBER_IMP_AUDIO.equals(arg);
            });

            ToHitData toHit = new ToHitData();
            toHit = ComputeAttackerToHitMods.compileCrewToHitMods(mockGame, mockInfantry, toHit, mockWeapon);

            assertEquals(0, toHit.getValue(), "Audio implant provides probe, not to-hit bonus");
        }

        @Test
        @DisplayName("Visual implant does not provide to-hit bonus")
        void visualImplant_noToHitBonus() {
            when(mockInfantry.hasAbility(anyString())).thenAnswer(invocation -> {
                String arg = invocation.getArgument(0);
                return OptionsConstants.MD_CYBER_IMP_VISUAL.equals(arg);
            });

            ToHitData toHit = new ToHitData();
            toHit = ComputeAttackerToHitMods.compileCrewToHitMods(mockGame, mockInfantry, toHit, mockWeapon);

            assertEquals(0, toHit.getValue(), "Visual implant provides probe, not to-hit bonus");
        }
    }

    @Nested
    @DisplayName("Multi-Modal Implant Tests")
    class MultiModalImplantTests {

        @Test
        @DisplayName("Infantry with MM implant gets -1 to-hit")
        void infantryWithMmImplant_getsMinusOneModifier() {
            when(mockInfantry.hasAbility(anyString())).thenAnswer(invocation -> {
                String arg = invocation.getArgument(0);
                return OptionsConstants.MD_MM_IMPLANTS.equals(arg);
            });

            ToHitData toHit = new ToHitData();
            toHit = ComputeAttackerToHitMods.compileCrewToHitMods(mockGame, mockInfantry, toHit, mockWeapon);

            assertEquals(-1, toHit.getValue());
            assertTrue(toHit.getDesc().contains("MD multi-modal implants"),
                  "Should show multi-modal implants message");
        }

        @Test
        @DisplayName("Non-infantry with MM implant only (no VDNI) gets no modifier")
        void nonInfantryWithMmImplantOnly_getsNoModifier() {
            Entity mockMek = mock(Entity.class);
            when(mockMek.isConventionalInfantry()).thenReturn(false);
            when(mockMek.getCrew()).thenReturn(mockCrew);
            when(mockMek.hasAbility(anyString())).thenAnswer(invocation -> {
                String arg = invocation.getArgument(0);
                return OptionsConstants.MD_MM_IMPLANTS.equals(arg);
            });

            ToHitData toHit = new ToHitData();
            toHit = ComputeAttackerToHitMods.compileCrewToHitMods(mockGame, mockMek, toHit, mockWeapon);

            assertEquals(0, toHit.getValue(),
                  "Non-infantry with MM implants but no VDNI should not get implant bonus");
        }

        @Test
        @DisplayName("Non-infantry with MM implant + VDNI gets -1 to-hit from implants")
        void nonInfantryWithMmImplantAndVdni_getsMinusOneFromImplants() {
            Entity mockMek = mock(Entity.class);
            when(mockMek.isConventionalInfantry()).thenReturn(false);
            when(mockMek.getCrew()).thenReturn(mockCrew);
            when(mockMek.hasAbility(anyString())).thenAnswer(invocation -> {
                String arg = invocation.getArgument(0);
                return OptionsConstants.MD_MM_IMPLANTS.equals(arg)
                      || OptionsConstants.MD_VDNI.equals(arg);
            });

            ToHitData toHit = new ToHitData();
            toHit = ComputeAttackerToHitMods.compileCrewToHitMods(mockGame, mockMek, toHit, mockWeapon);

            // VDNI gives -1, MM implants with VDNI also gives -1 = -2 total
            assertEquals(-2, toHit.getValue(),
                  "VDNI (-1) + MM implants synced via VDNI (-1) = -2 total");
        }

        @Test
        @DisplayName("Non-infantry with MM implant + BVDNI gets -1 to-hit from implants")
        void nonInfantryWithMmImplantAndBvdni_getsMinusOneFromImplants() {
            Entity mockMek = mock(Entity.class);
            when(mockMek.isConventionalInfantry()).thenReturn(false);
            when(mockMek.getCrew()).thenReturn(mockCrew);
            when(mockMek.hasAbility(anyString())).thenAnswer(invocation -> {
                String arg = invocation.getArgument(0);
                return OptionsConstants.MD_MM_IMPLANTS.equals(arg)
                      || OptionsConstants.MD_BVDNI.equals(arg);
            });

            ToHitData toHit = new ToHitData();
            toHit = ComputeAttackerToHitMods.compileCrewToHitMods(mockGame, mockMek, toHit, mockWeapon);

            // BVDNI gives -1, MM implants with BVDNI also gives -1 = -2 total
            assertEquals(-2, toHit.getValue(),
                  "BVDNI (-1) + MM implants synced via BVDNI (-1) = -2 total");
        }

        @Test
        @DisplayName("MM implants don't stack with laser/tele for infantry (still -1)")
        void infantryWithMmAndLaserImplants_getsOnlyMinusOne() {
            when(mockInfantry.hasAbility(anyString())).thenAnswer(invocation -> {
                String arg = invocation.getArgument(0);
                return OptionsConstants.MD_MM_IMPLANTS.equals(arg)
                      || OptionsConstants.MD_CYBER_IMP_LASER.equals(arg);
            });

            ToHitData toHit = new ToHitData();
            toHit = ComputeAttackerToHitMods.compileCrewToHitMods(mockGame, mockInfantry, toHit, mockWeapon);

            assertEquals(-1, toHit.getValue(),
                  "MM implants and laser implants should not stack - still only -1");
        }
    }

    @Nested
    @DisplayName("Enhanced Multi-Modal Implant Tests")
    class EnhancedMultiModalImplantTests {

        @Test
        @DisplayName("Infantry with Enhanced MM implant gets -1 to-hit")
        void infantryWithEnhMmImplant_getsMinusOneModifier() {
            when(mockInfantry.hasAbility(anyString())).thenAnswer(invocation -> {
                String arg = invocation.getArgument(0);
                return OptionsConstants.MD_ENH_MM_IMPLANTS.equals(arg);
            });

            ToHitData toHit = new ToHitData();
            toHit = ComputeAttackerToHitMods.compileCrewToHitMods(mockGame, mockInfantry, toHit, mockWeapon);

            assertEquals(-1, toHit.getValue());
            assertTrue(toHit.getDesc().contains("MD multi-modal implants"),
                  "Should show multi-modal implants message");
        }

        @Test
        @DisplayName("Non-infantry with Enhanced MM implant only (no VDNI) gets no modifier")
        void nonInfantryWithEnhMmImplantOnly_getsNoModifier() {
            Entity mockMek = mock(Entity.class);
            when(mockMek.isConventionalInfantry()).thenReturn(false);
            when(mockMek.getCrew()).thenReturn(mockCrew);
            when(mockMek.hasAbility(anyString())).thenAnswer(invocation -> {
                String arg = invocation.getArgument(0);
                return OptionsConstants.MD_ENH_MM_IMPLANTS.equals(arg);
            });

            ToHitData toHit = new ToHitData();
            toHit = ComputeAttackerToHitMods.compileCrewToHitMods(mockGame, mockMek, toHit, mockWeapon);

            assertEquals(0, toHit.getValue(),
                  "Non-infantry with Enhanced MM implants but no VDNI should not get implant bonus");
        }

        @Test
        @DisplayName("Non-infantry with Enhanced MM implant + VDNI gets -1 to-hit from implants")
        void nonInfantryWithEnhMmImplantAndVdni_getsMinusOneFromImplants() {
            Entity mockMek = mock(Entity.class);
            when(mockMek.isConventionalInfantry()).thenReturn(false);
            when(mockMek.getCrew()).thenReturn(mockCrew);
            when(mockMek.hasAbility(anyString())).thenAnswer(invocation -> {
                String arg = invocation.getArgument(0);
                return OptionsConstants.MD_ENH_MM_IMPLANTS.equals(arg)
                      || OptionsConstants.MD_VDNI.equals(arg);
            });

            ToHitData toHit = new ToHitData();
            toHit = ComputeAttackerToHitMods.compileCrewToHitMods(mockGame, mockMek, toHit, mockWeapon);

            // VDNI gives -1, Enhanced MM implants with VDNI also gives -1 = -2 total
            assertEquals(-2, toHit.getValue(),
                  "VDNI (-1) + Enhanced MM implants synced via VDNI (-1) = -2 total");
        }

        @Test
        @DisplayName("Enhanced MM implants don't stack with basic MM for infantry (still -1)")
        void infantryWithEnhMmAndBasicMmImplants_getsOnlyMinusOne() {
            when(mockInfantry.hasAbility(anyString())).thenAnswer(invocation -> {
                String arg = invocation.getArgument(0);
                return OptionsConstants.MD_ENH_MM_IMPLANTS.equals(arg)
                      || OptionsConstants.MD_MM_IMPLANTS.equals(arg);
            });

            ToHitData toHit = new ToHitData();
            toHit = ComputeAttackerToHitMods.compileCrewToHitMods(mockGame, mockInfantry, toHit, mockWeapon);

            assertEquals(-1, toHit.getValue(),
                  "Enhanced MM and basic MM implants should not stack - still only -1");
        }
    }
}

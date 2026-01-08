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
package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for Full-Head Ejection Pod (FHEP) functionality per TO:AUE p.121 rules.
 *
 * <p>FHEP allows BattleMek and IndustrialMek crews to escape during any phase.
 * Key differences from standard ejection:</p>
 * <ul>
 *   <li>MekWarrior takes 1 automatic damage on launch</li>
 *   <li>12 hex landing range (forward arc only if prone)</li>
 *   <li>PSR +3 to land on target; scatter 1d6/2 hexes on failure</li>
 *   <li>Additional ejection roll +2 for second damage point</li>
 *   <li>Uses head's armor/structure values (not simple threshold)</li>
 *   <li>If submerged: rockets to surface and floats as displacement hull</li>
 * </ul>
 */
class FullHeadEjectionPodTest {

    private Crew mockCrew;
    private Mek mockMek;
    private Player mockPlayer;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        mockCrew = mock(Crew.class);
        mockMek = mock(Mek.class);
        mockPlayer = mock(Player.class);

        // Mock Player methods required by Entity.setOwner()
        when(mockPlayer.getId()).thenReturn(1);
        when(mockPlayer.getName()).thenReturn("Test Player");

        // Mock Crew methods required by EjectedCrew constructor
        when(mockCrew.getSize()).thenReturn(1);
        when(mockCrew.getName()).thenReturn("Test MekWarrior");
        when(mockCrew.getSlotCount()).thenReturn(1);
        when(mockCrew.isDead()).thenReturn(false);

        // Default mock setup - Mek with head armor 9, structure 3
        when(mockMek.getOArmor(Mek.LOC_HEAD)).thenReturn(9);
        when(mockMek.getOInternal(Mek.LOC_HEAD)).thenReturn(3);
        when(mockMek.getArmor(Mek.LOC_HEAD)).thenReturn(9);
        when(mockMek.getInternal(Mek.LOC_HEAD)).thenReturn(3);
        when(mockMek.getDisplayName()).thenReturn("Test Atlas");
        when(mockMek.getCrew()).thenReturn(mockCrew);
        when(mockMek.getOwner()).thenReturn(mockPlayer);
        when(mockMek.getId()).thenReturn(1);
        when(mockMek.getExternalIdAsString()).thenReturn("-1");
        when(mockMek.getGame()).thenReturn(null);
        when(mockMek.getInitiative()).thenReturn(null);
    }

    @Nested
    @DisplayName("FHEP Creation Tests")
    class FhepCreationTests {

        @Test
        @DisplayName("FHEP copies head armor/structure from source Mek")
        void fhep_CopiesHeadArmorStructure() {
            when(mockMek.getOArmor(Mek.LOC_HEAD)).thenReturn(9);
            when(mockMek.getOInternal(Mek.LOC_HEAD)).thenReturn(3);
            when(mockMek.getArmor(Mek.LOC_HEAD)).thenReturn(7);  // Some armor already damaged
            when(mockMek.getInternal(Mek.LOC_HEAD)).thenReturn(3);

            FullHeadEjectionPod fhep = new FullHeadEjectionPod(mockMek, new Coords(5, 5));

            assertEquals(9, fhep.getOriginalHeadArmor(),
                  "FHEP should copy original head armor");
            assertEquals(3, fhep.getOriginalHeadInternalStructure(),
                  "FHEP should copy original head internal structure");
            assertEquals(7, fhep.getCurrentHeadArmor(),
                  "FHEP should copy current head armor");
            assertEquals(3, fhep.getCurrentHeadInternalStructure(),
                  "FHEP should copy current head internal structure");
        }

        @Test
        @DisplayName("FHEP uses correct sprite name")
        void fhep_UsesCorrectSpriteName() {
            FullHeadEjectionPod fhep = new FullHeadEjectionPod(mockMek, new Coords(5, 5));

            assertEquals(FullHeadEjectionPod.FHEP_SPRITE_NAME, fhep.getChassis(),
                  "FHEP should use 'Full Head Escape Pod' chassis for sprite lookup");
        }

        @Test
        @DisplayName("FHEP uses correct display name")
        void fhep_UsesCorrectDisplayName() {
            FullHeadEjectionPod fhep = new FullHeadEjectionPod(mockMek, new Coords(5, 5));

            assertTrue(fhep.getDisplayName().contains(FullHeadEjectionPod.FHEP_DISPLAY_NAME),
                  "FHEP should use 'FHEP' in display name");
            assertTrue(fhep.getDisplayName().contains("Test Atlas"),
                  "FHEP should include source Mek name in display name");
        }

        @Test
        @DisplayName("FHEP is always immobile")
        void fhep_AlwaysImmobile() {
            FullHeadEjectionPod fhep = new FullHeadEjectionPod(mockMek, new Coords(5, 5));

            assertTrue(fhep.isImmobile(),
                  "FHEP should always be immobile for targeting purposes");

            // Even if crew exits, the pod itself is still immobile
            fhep.setCrewInside(false);
            assertTrue(fhep.isImmobile(),
                  "Empty FHEP should still be immobile");
        }

        @Test
        @DisplayName("FHEP is not conventional infantry")
        void fhep_NotConventionalInfantry() {
            FullHeadEjectionPod fhep = new FullHeadEjectionPod(mockMek, new Coords(5, 5));

            assertFalse(fhep.isConventionalInfantry(),
                  "FHEP should not be conventional infantry (uses head damage model)");
        }
    }

    @Nested
    @DisplayName("FHEP Damage Model Tests (TO:AUE p.121)")
    class FhepDamageModelTests {

        private FullHeadEjectionPod createTestFhep() {
            // Create FHEP with 9 armor, 3 structure
            when(mockMek.getArmor(Mek.LOC_HEAD)).thenReturn(9);
            when(mockMek.getInternal(Mek.LOC_HEAD)).thenReturn(3);
            return new FullHeadEjectionPod(mockMek, new Coords(5, 5));
        }

        @Test
        @DisplayName("New FHEP has full armor and structure")
        void newFhep_HasFullArmorStructure() {
            FullHeadEjectionPod fhep = createTestFhep();

            assertEquals(9, fhep.getCurrentHeadArmor(),
                  "New FHEP should have full armor");
            assertEquals(3, fhep.getCurrentHeadInternalStructure(),
                  "New FHEP should have full structure");
            assertFalse(fhep.isBreached(),
                  "New FHEP should not be breached");
        }

        @Test
        @DisplayName("Damage applies to armor first")
        void fhep_DamageAppliesToArmorFirst() {
            FullHeadEjectionPod fhep = createTestFhep();

            boolean breached = fhep.applyDamage(5);

            assertFalse(breached, "5 damage should not breach pod with 9 armor");
            assertEquals(4, fhep.getCurrentHeadArmor(),
                  "Armor should be reduced by 5 (9 - 5 = 4)");
            assertEquals(3, fhep.getCurrentHeadInternalStructure(),
                  "Structure should be unchanged");
        }

        @Test
        @DisplayName("Excess damage carries to structure")
        void fhep_ExcessDamageCarriesToStructure() {
            FullHeadEjectionPod fhep = createTestFhep();

            boolean breached = fhep.applyDamage(10);  // 9 armor + 1 structure

            assertFalse(breached, "10 damage should not breach (9 armor + 1 of 3 structure)");
            assertEquals(0, fhep.getCurrentHeadArmor(),
                  "Armor should be depleted");
            assertEquals(2, fhep.getCurrentHeadInternalStructure(),
                  "Structure should be reduced by 1 (3 - 1 = 2)");
        }

        @Test
        @DisplayName("Pod is breached when structure reaches 0")
        void fhep_BreachedWhenStructureReaches0() {
            FullHeadEjectionPod fhep = createTestFhep();

            boolean breached = fhep.applyDamage(12);  // 9 armor + 3 structure

            assertTrue(breached, "12 damage should breach pod (9 armor + 3 structure)");
            assertEquals(0, fhep.getCurrentHeadArmor(),
                  "Armor should be depleted");
            assertEquals(0, fhep.getCurrentHeadInternalStructure(),
                  "Structure should be depleted");
            assertTrue(fhep.isBreached(),
                  "Pod should be marked as breached");
        }

        @Test
        @DisplayName("Cumulative damage works correctly")
        void fhep_CumulativeDamageWorks() {
            FullHeadEjectionPod fhep = createTestFhep();

            fhep.applyDamage(5);  // 4 armor remaining
            assertEquals(4, fhep.getCurrentHeadArmor());

            fhep.applyDamage(3);  // 1 armor remaining
            assertEquals(1, fhep.getCurrentHeadArmor());

            fhep.applyDamage(2);  // 0 armor, 2 structure remaining
            assertEquals(0, fhep.getCurrentHeadArmor());
            assertEquals(2, fhep.getCurrentHeadInternalStructure());

            boolean breached = fhep.applyDamage(2);  // 0 structure
            assertTrue(breached, "Final damage should breach the pod");
            assertTrue(fhep.isBreached());
        }

        @Test
        @DisplayName("Breached FHEP ignores additional damage")
        void fhep_BreachedIgnoresAdditionalDamage() {
            FullHeadEjectionPod fhep = createTestFhep();

            fhep.applyDamage(12);  // Breach
            int armorAfterBreach = fhep.getCurrentHeadArmor();
            int structureAfterBreach = fhep.getCurrentHeadInternalStructure();

            boolean result = fhep.applyDamage(5);  // Additional damage after breach

            assertFalse(result, "applyDamage should return false when already breached");
            assertEquals(armorAfterBreach, fhep.getCurrentHeadArmor(),
                  "Armor should not change after breach");
            assertEquals(structureAfterBreach, fhep.getCurrentHeadInternalStructure(),
                  "Structure should not change after breach");
        }

        @Test
        @DisplayName("Zero damage does not affect FHEP")
        void fhep_ZeroDamage_NoEffect() {
            FullHeadEjectionPod fhep = createTestFhep();

            boolean breached = fhep.applyDamage(0);

            assertFalse(breached, "Zero damage should not breach FHEP");
            assertEquals(9, fhep.getCurrentHeadArmor(),
                  "Armor should be unchanged");
        }

        @Test
        @DisplayName("Negative damage does not affect FHEP")
        void fhep_NegativeDamage_NoEffect() {
            FullHeadEjectionPod fhep = createTestFhep();

            boolean breached = fhep.applyDamage(-5);

            assertFalse(breached, "Negative damage should not breach FHEP");
            assertEquals(9, fhep.getCurrentHeadArmor(),
                  "Armor should be unchanged");
        }
    }

    @Nested
    @DisplayName("FHEP Floating Mode Tests (TO:AUE p.121)")
    class FhepFloatingModeTests {

        @Test
        @DisplayName("FHEP can be set to floating mode")
        void fhep_CanBeSetToFloating() {
            FullHeadEjectionPod fhep = new FullHeadEjectionPod(mockMek, new Coords(5, 5));

            fhep.setFloating(true);

            assertTrue(fhep.isFloating(),
                  "FHEP should be floating after setFloating(true)");
            assertEquals(EntityMovementMode.NAVAL, fhep.getMovementMode(),
                  "Floating FHEP should use NAVAL movement mode");
        }

        @Test
        @DisplayName("FHEP floating mode is cleared when crew exits")
        void fhep_FloatingClearedWhenCrewExits() {
            FullHeadEjectionPod fhep = new FullHeadEjectionPod(mockMek, new Coords(5, 5));
            fhep.setFloating(true);

            fhep.setCrewInside(false);

            assertFalse(fhep.isFloating(),
                  "FHEP should not be floating after crew exits");
        }
    }

    @Nested
    @DisplayName("FHEP Crew Exit Tests")
    class FhepCrewExitTests {

        @Test
        @DisplayName("Breached FHEP cannot have crew exit")
        void fhep_Breached_CrewCannotExit() {
            FullHeadEjectionPod fhep = new FullHeadEjectionPod(mockMek, new Coords(5, 5));
            fhep.applyDamage(12);  // Breach the pod

            assertFalse(fhep.canCrewExit(),
                  "Crew cannot exit a breached FHEP (they are dead)");
        }

        @Test
        @DisplayName("FHEP with no crew inside cannot have crew exit")
        void fhep_NoCrewInside_CrewCannotExit() {
            FullHeadEjectionPod fhep = new FullHeadEjectionPod(mockMek, new Coords(5, 5));
            fhep.setCrewInside(false);

            assertFalse(fhep.canCrewExit(),
                  "Crew cannot exit if already outside");
        }
    }
}

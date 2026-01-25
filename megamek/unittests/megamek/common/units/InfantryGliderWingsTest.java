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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.enums.ProstheticEnhancementType;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.planetaryConditions.PlanetaryConditions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for MD_PL_GLIDER (Prosthetic Wings, Glider) cybernetic implant functionality
 * on Infantry units per IO p.85.
 *
 * <p>Rules implemented:
 * <ul>
 *   <li>Infantry can dismount from VTOLs as jump infantry (canAssaultDrop)</li>
 *   <li>Cannot be used in vacuum or trace (very thin) atmospheres</li>
 *   <li>Protects against fall damage (walking off 2+ levels, displacement)</li>
 *   <li>Conventional infantry only (not battle armor)</li>
 *   <li>No BV impact</li>
 * </ul>
 */
class InfantryGliderWingsTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    /**
     * Creates an Infantry unit with the specified movement mode and optional glider wings ability.
     */
    private Infantry createInfantry(EntityMovementMode movementMode, boolean hasGliderWings) {
        Infantry infantry = new Infantry();
        infantry.setId(1);
        infantry.setMovementMode(movementMode);
        infantry.setSquadSize(7);
        infantry.setSquadCount(4);
        infantry.autoSetInternal();

        Crew crew = new Crew(CrewType.INFANTRY_CREW);
        crew.setGunnery(4, crew.getCrewType().getGunnerPos());
        crew.setPiloting(5, crew.getCrewType().getPilotPos());
        crew.setName("Test Crew", 0);

        PilotOptions options = new PilotOptions();
        if (hasGliderWings) {
            options.getOption(OptionsConstants.MD_PL_GLIDER).setValue(true);
        }
        crew.setOptions(options);
        infantry.setCrew(crew);

        return infantry;
    }

    /**
     * Sets up a mock game with the specified atmosphere and game options.
     */
    private Game createGameWithAtmosphere(Atmosphere atmosphere) {
        Game mockGame = mock(Game.class);
        PlanetaryConditions conditions = new PlanetaryConditions();
        conditions.setAtmosphere(atmosphere);
        when(mockGame.getPlanetaryConditions()).thenReturn(conditions);

        // Mock game options to prevent NPE in gameOptions()
        GameOptions mockGameOptions = mock(GameOptions.class);
        when(mockGame.getOptions()).thenReturn(mockGameOptions);
        when(mockGameOptions.booleanOption(anyString())).thenReturn(false);

        return mockGame;
    }

    @Nested
    @DisplayName("Atmosphere Restriction Tests (canUseGliderWings)")
    class AtmosphereRestrictionTests {

        @Test
        @DisplayName("Glider wings work in standard atmosphere")
        void gliderWingsWorkInStandardAtmosphere() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.STANDARD));

            assertTrue(infantry.canUseGliderWings());
        }

        @Test
        @DisplayName("Glider wings work in thin atmosphere")
        void gliderWingsWorkInThinAtmosphere() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.THIN));

            assertTrue(infantry.canUseGliderWings());
        }

        @Test
        @DisplayName("Glider wings work in high atmosphere")
        void gliderWingsWorkInHighAtmosphere() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.HIGH));

            assertTrue(infantry.canUseGliderWings());
        }

        @Test
        @DisplayName("Glider wings work in very high atmosphere")
        void gliderWingsWorkInVeryHighAtmosphere() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.VERY_HIGH));

            assertTrue(infantry.canUseGliderWings());
        }

        @Test
        @DisplayName("Glider wings do NOT work in trace atmosphere")
        void gliderWingsDoNotWorkInTraceAtmosphere() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.TRACE));

            assertFalse(infantry.canUseGliderWings());
        }

        @Test
        @DisplayName("Glider wings do NOT work in vacuum")
        void gliderWingsDoNotWorkInVacuum() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.VACUUM));

            assertFalse(infantry.canUseGliderWings());
        }

        @Test
        @DisplayName("Glider wings work when no game context (lobby/loading)")
        void gliderWingsWorkWithNoGameContext() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);
            // No game set

            assertTrue(infantry.canUseGliderWings());
        }
    }

    @Nested
    @DisplayName("Fall Damage Protection Tests (isProtectedFromFallDamage)")
    class FallDamageProtectionTests {

        @Test
        @DisplayName("Conventional infantry with glider wings in standard atmosphere is protected")
        void conventionalInfantryWithGliderWingsProtected() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.STANDARD));

            assertTrue(infantry.isProtectedFromFallDamage());
        }

        @Test
        @DisplayName("Conventional infantry without glider wings is NOT protected")
        void conventionalInfantryWithoutGliderWingsNotProtected() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, false);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.STANDARD));

            assertFalse(infantry.isProtectedFromFallDamage());
        }

        @Test
        @DisplayName("Infantry with glider wings in vacuum is NOT protected")
        void infantryWithGliderWingsInVacuumNotProtected() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.VACUUM));

            assertFalse(infantry.isProtectedFromFallDamage());
        }

        @Test
        @DisplayName("Infantry with glider wings in trace atmosphere is NOT protected")
        void infantryWithGliderWingsInTraceNotProtected() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.TRACE));

            assertFalse(infantry.isProtectedFromFallDamage());
        }

        @Test
        @DisplayName("Jump infantry with glider wings is protected")
        void jumpInfantryWithGliderWingsProtected() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_JUMP, true);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.STANDARD));

            assertTrue(infantry.isProtectedFromFallDamage());
        }

        @Test
        @DisplayName("Motorized infantry with glider wings is protected")
        void motorizedInfantryWithGliderWingsProtected() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_MOTORIZED, true);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.STANDARD));

            assertTrue(infantry.isProtectedFromFallDamage());
        }
    }

    @Nested
    @DisplayName("Assault Drop Tests (canAssaultDrop)")
    class AssaultDropTests {

        @Test
        @DisplayName("Leg infantry with glider wings in standard atmosphere can assault drop")
        void legInfantryWithGliderWingsCanAssaultDrop() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.STANDARD));

            assertTrue(infantry.canAssaultDrop());
        }

        @Test
        @DisplayName("Leg infantry without glider wings cannot assault drop")
        void legInfantryWithoutGliderWingsCannotAssaultDrop() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, false);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.STANDARD));

            assertFalse(infantry.canAssaultDrop());
        }

        @Test
        @DisplayName("Jump infantry can assault drop (inherent ability)")
        void jumpInfantryCanAssaultDrop() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_JUMP, false);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.STANDARD));

            assertTrue(infantry.canAssaultDrop());
        }

        @Test
        @DisplayName("Leg infantry with glider wings in vacuum cannot assault drop")
        void legInfantryWithGliderWingsInVacuumCannotAssaultDrop() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.VACUUM));

            assertFalse(infantry.canAssaultDrop());
        }

        @Test
        @DisplayName("Leg infantry with glider wings in trace atmosphere cannot assault drop")
        void legInfantryWithGliderWingsInTraceCannotAssaultDrop() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.TRACE));

            assertFalse(infantry.canAssaultDrop());
        }

        @Test
        @DisplayName("Motorized infantry with glider wings can assault drop")
        void motorizedInfantryWithGliderWingsCanAssaultDrop() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_MOTORIZED, true);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.STANDARD));

            assertTrue(infantry.canAssaultDrop());
        }

        @Test
        @DisplayName("VTOL infantry can assault drop (inherent ability)")
        void vtolInfantryCanAssaultDrop() {
            Infantry infantry = createInfantry(EntityMovementMode.VTOL, false);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.STANDARD));

            assertTrue(infantry.canAssaultDrop());
        }

        @Test
        @DisplayName("Hover infantry can assault drop (inherent ability)")
        void hoverInfantryCanAssaultDrop() {
            Infantry infantry = createInfantry(EntityMovementMode.HOVER, false);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.STANDARD));

            assertTrue(infantry.canAssaultDrop());
        }
    }

    @Nested
    @DisplayName("Construction Rule Tests (IO p.85)")
    class ConstructionRuleTests {

        /**
         * Creates infantry with both wing types enabled for testing mutual exclusivity.
         */
        private Infantry createInfantryWithBothWings() {
            Infantry infantry = new Infantry();
            infantry.setId(1);
            infantry.setMovementMode(EntityMovementMode.INF_LEG);
            infantry.setSquadSize(7);
            infantry.setSquadCount(4);
            infantry.autoSetInternal();

            Crew crew = new Crew(CrewType.INFANTRY_CREW);
            crew.setGunnery(4, crew.getCrewType().getGunnerPos());
            crew.setPiloting(5, crew.getCrewType().getPilotPos());
            crew.setName("Test Crew", 0);

            PilotOptions options = new PilotOptions();
            options.getOption(OptionsConstants.MD_PL_GLIDER).setValue(true);
            options.getOption(OptionsConstants.MD_PL_FLIGHT).setValue(true);
            crew.setOptions(options);
            infantry.setCrew(crew);

            return infantry;
        }

        @Test
        @DisplayName("Glider wings and powered flight wings are mutually exclusive")
        void gliderAndFlightWingsMutuallyExclusive() {
            Infantry infantry = createInfantryWithBothWings();

            assertTrue(infantry.hasInvalidWingsConfiguration());
        }

        @Test
        @DisplayName("Glider wings alone is valid configuration")
        void gliderWingsAloneIsValid() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);

            assertFalse(infantry.hasInvalidWingsConfiguration());
        }

        @Test
        @DisplayName("Without glider wings, max extraneous limb pairs is 2")
        void withoutGliderWingsMaxPairsIsTwo() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, false);

            assertEquals(2, infantry.getMaxExtraneousLimbPairs());
        }

        @Test
        @DisplayName("With glider wings, max extraneous limb pairs is 1")
        void withGliderWingsMaxPairsIsOne() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);

            assertEquals(1, infantry.getMaxExtraneousLimbPairs());
        }

        @Test
        @DisplayName("Glider wings with two extraneous pairs is invalid")
        void gliderWingsWithTwoExtraneousPairsInvalid() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);
            infantry.setExtraneousPair1(ProstheticEnhancementType.CLIMBING_CLAWS);
            infantry.setExtraneousPair2(ProstheticEnhancementType.GRAPPLER);

            assertTrue(infantry.hasExcessiveExtraneousLimbs());
        }

        @Test
        @DisplayName("Glider wings with one extraneous pair is valid")
        void gliderWingsWithOneExtraneousPairValid() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);
            infantry.setExtraneousPair1(ProstheticEnhancementType.CLIMBING_CLAWS);

            assertFalse(infantry.hasExcessiveExtraneousLimbs());
        }

        @Test
        @DisplayName("Without glider wings, two extraneous pairs is valid")
        void withoutGliderWingsTwoExtraneousPairsValid() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, false);
            infantry.setExtraneousPair1(ProstheticEnhancementType.CLIMBING_CLAWS);
            infantry.setExtraneousPair2(ProstheticEnhancementType.GRAPPLER);

            assertFalse(infantry.hasExcessiveExtraneousLimbs());
        }

        @Test
        @DisplayName("Foot infantry with glider wings is valid configuration")
        void footInfantryWithGliderWingsValid() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);

            assertFalse(infantry.hasGliderWingsOnInvalidInfantryType());
        }

        @Test
        @DisplayName("Jump infantry with glider wings is valid configuration")
        void jumpInfantryWithGliderWingsValid() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_JUMP, true);

            assertFalse(infantry.hasGliderWingsOnInvalidInfantryType());
        }

        @Test
        @DisplayName("Motorized infantry with glider wings is INVALID")
        void motorizedInfantryWithGliderWingsInvalid() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_MOTORIZED, true);

            assertTrue(infantry.hasGliderWingsOnInvalidInfantryType());
        }

        @Test
        @DisplayName("Wheeled infantry with glider wings is INVALID (mechanized)")
        void wheeledInfantryWithGliderWingsInvalid() {
            Infantry infantry = createInfantry(EntityMovementMode.WHEELED, true);

            assertTrue(infantry.hasGliderWingsOnInvalidInfantryType());
        }

        @Test
        @DisplayName("Tracked infantry with glider wings is INVALID (mechanized)")
        void trackedInfantryWithGliderWingsInvalid() {
            Infantry infantry = createInfantry(EntityMovementMode.TRACKED, true);

            assertTrue(infantry.hasGliderWingsOnInvalidInfantryType());
        }

        @Test
        @DisplayName("Beast-mounted infantry with glider wings is INVALID")
        void beastMountedInfantryWithGliderWingsInvalid() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);
            infantry.setMount(InfantryMount.HORSE);

            assertTrue(infantry.hasGliderWingsOnInvalidInfantryType());
        }

        @Test
        @DisplayName("Infantry without glider wings always passes infantry type check")
        void infantryWithoutGliderWingsAlwaysValid() {
            Infantry motorized = createInfantry(EntityMovementMode.INF_MOTORIZED, false);
            Infantry tracked = createInfantry(EntityMovementMode.TRACKED, false);

            assertFalse(motorized.hasGliderWingsOnInvalidInfantryType());
            assertFalse(tracked.hasGliderWingsOnInvalidInfantryType());
        }
    }
}

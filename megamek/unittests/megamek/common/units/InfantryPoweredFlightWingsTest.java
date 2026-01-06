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
 * Tests for MD_PL_FLIGHT (Prosthetic Wings, Powered Flight) cybernetic implant functionality on Infantry units per IO
 * p.85.
 *
 * <p>Rules implemented:</p>
 * <ul>
 *   <li>Provides 2 MPs of VTOL movement</li>
 *   <li>Includes all benefits of glider wings (assault drop, fall protection)</li>
 *   <li>Cannot be used in vacuum</li>
 *   <li>Cannot combine with glider wings</li>
 *   <li>Limits extraneous limb pairs to 1</li>
 *   <li>Conventional infantry only (not battle armor)</li>
 *   <li>BV impact: 2 VTOL MP contributes to defensive TMM factor (unlike glider wings which have no BV impact)</li>
 * </ul>
 */
class InfantryPoweredFlightWingsTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    /**
     * Creates an Infantry unit with the specified movement mode and optional powered flight wings ability.
     */
    private Infantry createInfantry(EntityMovementMode movementMode, boolean hasPoweredFlightWings) {
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
        if (hasPoweredFlightWings) {
            options.getOption(OptionsConstants.MD_PL_FLIGHT).setValue(true);
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
    @DisplayName("VTOL Movement Tests (getPoweredFlightMP)")
    class VTOLMovementTests {

        @Test
        @DisplayName("Powered flight wings provide 2 VTOL MP in standard atmosphere")
        void poweredFlightProvides2MPInStandardAtmosphere() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.STANDARD));

            assertEquals(2, infantry.getPoweredFlightMP());
        }

        @Test
        @DisplayName("Powered flight wings provide 2 VTOL MP in thin atmosphere")
        void poweredFlightProvides2MPInThinAtmosphere() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.THIN));

            assertEquals(2, infantry.getPoweredFlightMP());
        }

        @Test
        @DisplayName("Powered flight wings provide 0 MP in vacuum")
        void poweredFlightProvides0MPInVacuum() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.VACUUM));

            assertEquals(0, infantry.getPoweredFlightMP());
        }

        @Test
        @DisplayName("Powered flight wings provide 0 MP in trace atmosphere")
        void poweredFlightProvides0MPInTrace() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.TRACE));

            assertEquals(0, infantry.getPoweredFlightMP());
        }

        @Test
        @DisplayName("Infantry without powered flight wings gets 0 MP")
        void infantryWithoutPoweredFlightGets0MP() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, false);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.STANDARD));

            assertEquals(0, infantry.getPoweredFlightMP());
        }

        @Test
        @DisplayName("Powered flight wings work when no game context (lobby/loading)")
        void poweredFlightWorksWithNoGameContext() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);
            // No game set

            assertEquals(2, infantry.getPoweredFlightMP());
        }
    }

    @Nested
    @DisplayName("Atmosphere Restriction Tests (canUsePoweredFlightWings)")
    class AtmosphereRestrictionTests {

        @Test
        @DisplayName("Powered flight works in standard atmosphere")
        void poweredFlightWorksInStandardAtmosphere() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.STANDARD));

            assertTrue(infantry.canUsePoweredFlightWings());
        }

        @Test
        @DisplayName("Powered flight works in thin atmosphere")
        void poweredFlightWorksInThinAtmosphere() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.THIN));

            assertTrue(infantry.canUsePoweredFlightWings());
        }

        @Test
        @DisplayName("Powered flight does NOT work in trace atmosphere")
        void poweredFlightDoesNotWorkInTraceAtmosphere() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.TRACE));

            assertFalse(infantry.canUsePoweredFlightWings());
        }

        @Test
        @DisplayName("Powered flight does NOT work in vacuum")
        void poweredFlightDoesNotWorkInVacuum() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.VACUUM));

            assertFalse(infantry.canUsePoweredFlightWings());
        }
    }

    @Nested
    @DisplayName("Fall Damage Protection Tests (isProtectedFromFallDamage)")
    class FallDamageProtectionTests {

        @Test
        @DisplayName("Conventional infantry with powered flight in standard atmosphere is protected")
        void conventionalInfantryWithPoweredFlightProtected() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.STANDARD));

            assertTrue(infantry.isProtectedFromFallDamage());
        }

        @Test
        @DisplayName("Conventional infantry without powered flight is NOT protected")
        void conventionalInfantryWithoutPoweredFlightNotProtected() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, false);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.STANDARD));

            assertFalse(infantry.isProtectedFromFallDamage());
        }

        @Test
        @DisplayName("Infantry with powered flight in vacuum is NOT protected")
        void infantryWithPoweredFlightInVacuumNotProtected() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.VACUUM));

            assertFalse(infantry.isProtectedFromFallDamage());
        }
    }

    @Nested
    @DisplayName("Assault Drop Tests (canAssaultDrop)")
    class AssaultDropTests {

        @Test
        @DisplayName("Leg infantry with powered flight in standard atmosphere can assault drop")
        void legInfantryWithPoweredFlightCanAssaultDrop() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.STANDARD));

            assertTrue(infantry.canAssaultDrop());
        }

        @Test
        @DisplayName("Leg infantry without powered flight cannot assault drop")
        void legInfantryWithoutPoweredFlightCannotAssaultDrop() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, false);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.STANDARD));

            assertFalse(infantry.canAssaultDrop());
        }

        @Test
        @DisplayName("Leg infantry with powered flight in vacuum cannot assault drop")
        void legInfantryWithPoweredFlightInVacuumCannotAssaultDrop() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.VACUUM));

            assertFalse(infantry.canAssaultDrop());
        }

        @Test
        @DisplayName("Motorized infantry with powered flight can assault drop")
        void motorizedInfantryWithPoweredFlightCanAssaultDrop() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_MOTORIZED, true);
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
        @DisplayName("Powered flight and glider wings are mutually exclusive")
        void poweredFlightAndGliderWingsMutuallyExclusive() {
            Infantry infantry = createInfantryWithBothWings();

            assertTrue(infantry.hasInvalidWingsConfiguration());
        }

        @Test
        @DisplayName("Powered flight alone is valid configuration")
        void poweredFlightAloneIsValid() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);

            assertFalse(infantry.hasInvalidWingsConfiguration());
        }

        @Test
        @DisplayName("Without powered flight, max extraneous limb pairs is 2")
        void withoutPoweredFlightMaxPairsIsTwo() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, false);

            assertEquals(2, infantry.getMaxExtraneousLimbPairs());
        }

        @Test
        @DisplayName("With powered flight, max extraneous limb pairs is 1")
        void withPoweredFlightMaxPairsIsOne() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);

            assertEquals(1, infantry.getMaxExtraneousLimbPairs());
        }

        @Test
        @DisplayName("Powered flight with two extraneous pairs is invalid")
        void poweredFlightWithTwoExtraneousPairsInvalid() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);
            infantry.setExtraneousPair1(ProstheticEnhancementType.CLIMBING_CLAWS);
            infantry.setExtraneousPair2(ProstheticEnhancementType.GRAPPLER);

            assertTrue(infantry.hasExcessiveExtraneousLimbs());
        }

        @Test
        @DisplayName("Powered flight with one extraneous pair is valid")
        void poweredFlightWithOneExtraneousPairValid() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);
            infantry.setExtraneousPair1(ProstheticEnhancementType.CLIMBING_CLAWS);

            assertFalse(infantry.hasExcessiveExtraneousLimbs());
        }

        @Test
        @DisplayName("Without powered flight, two extraneous pairs is valid")
        void withoutPoweredFlightTwoExtraneousPairsValid() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, false);
            infantry.setExtraneousPair1(ProstheticEnhancementType.CLIMBING_CLAWS);
            infantry.setExtraneousPair2(ProstheticEnhancementType.GRAPPLER);

            assertFalse(infantry.hasExcessiveExtraneousLimbs());
        }
    }

    @Nested
    @DisplayName("hasPoweredFlightWings Tests")
    class HasPoweredFlightWingsTests {

        @Test
        @DisplayName("Conventional infantry with ability returns true")
        void conventionalInfantryWithAbilityReturnsTrue() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);

            assertTrue(infantry.hasPoweredFlightWings());
        }

        @Test
        @DisplayName("Conventional infantry without ability returns false")
        void conventionalInfantryWithoutAbilityReturnsFalse() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, false);

            assertFalse(infantry.hasPoweredFlightWings());
        }

        @Test
        @DisplayName("Jump infantry with powered flight returns true")
        void jumpInfantryWithPoweredFlightReturnsTrue() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_JUMP, true);

            assertTrue(infantry.hasPoweredFlightWings());
        }

        @Test
        @DisplayName("Motorized infantry with powered flight returns true")
        void motorizedInfantryWithPoweredFlightReturnsTrue() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_MOTORIZED, true);

            assertTrue(infantry.hasPoweredFlightWings());
        }
    }

    @Nested
    @DisplayName("hasVTOLMovementCapability Tests")
    class HasVTOLMovementCapabilityTests {

        @Test
        @DisplayName("Leg infantry with powered flight has VTOL capability")
        void legInfantryWithPoweredFlightHasVTOLCapability() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.STANDARD));

            assertTrue(infantry.hasVTOLMovementCapability());
        }

        @Test
        @DisplayName("Leg infantry without powered flight has no VTOL capability")
        void legInfantryWithoutPoweredFlightNoVTOLCapability() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, false);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.STANDARD));

            assertFalse(infantry.hasVTOLMovementCapability());
        }

        @Test
        @DisplayName("VTOL infantry has VTOL capability inherently")
        void vtolInfantryHasVTOLCapabilityInherently() {
            Infantry infantry = createInfantry(EntityMovementMode.VTOL, false);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.STANDARD));

            assertTrue(infantry.hasVTOLMovementCapability());
        }

        @Test
        @DisplayName("Powered flight in vacuum has no VTOL capability")
        void poweredFlightInVacuumNoVTOLCapability() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.VACUUM));

            assertFalse(infantry.hasVTOLMovementCapability());
        }
    }

    @Nested
    @DisplayName("getVTOLMP Tests")
    class GetVTOLMPTests {

        @Test
        @DisplayName("Powered flight infantry returns 2 VTOL MP")
        void poweredFlightReturns2VTOLMP() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.STANDARD));

            assertEquals(2, infantry.getVTOLMP());
        }

        @Test
        @DisplayName("Infantry without powered flight returns 0 VTOL MP")
        void withoutPoweredFlightReturns0VTOLMP() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, false);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.STANDARD));

            assertEquals(0, infantry.getVTOLMP());
        }

        @Test
        @DisplayName("Powered flight in vacuum returns 0 VTOL MP")
        void poweredFlightInVacuumReturns0VTOLMP() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true);
            infantry.setGame(createGameWithAtmosphere(Atmosphere.VACUUM));

            assertEquals(0, infantry.getVTOLMP());
        }
    }
}

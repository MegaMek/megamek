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
package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.PlanetaryConditions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for infantry fast movement per TO:AR p.25.
 *
 * <p>Rules:</p>
 * <ul>
 *   <li>Any infantry can forgo attacks to gain +1 Ground MP (MOVE_RUN)</li>
 *   <li>0 MP infantry get +2 run MP (1 free hex + 1 fast movement bonus)</li>
 *   <li>0 MP infantry that use fast movement cannot move or fire in the following turn</li>
 * </ul>
 */
class InfantryFastMovementTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private Infantry createInfantry(int walkMP) {
        Infantry infantry = new Infantry();
        infantry.setId(1);
        infantry.setMovementMode(EntityMovementMode.INF_LEG);
        infantry.setSquadSize(7);
        infantry.setSquadCount(4);
        infantry.autoSetInternal();
        infantry.setOriginalWalkMP(walkMP);
        infantry.setDeployed(true);

        Crew crew = new Crew(CrewType.INFANTRY_CREW);
        crew.setGunnery(4, crew.getCrewType().getGunnerPos());
        crew.setPiloting(5, crew.getCrewType().getPilotPos());
        crew.setName("Test Crew", 0);
        infantry.setCrew(crew);

        return infantry;
    }

    private Game createGameWithFastMovement(boolean enabled) {
        Game mockGame = mock(Game.class);
        GameOptions mockOptions = mock(GameOptions.class);
        when(mockGame.getOptions()).thenReturn(mockOptions);
        // Default all boolean options to false, then override specific ones
        when(mockOptions.booleanOption(anyString())).thenReturn(false);
        when(mockOptions.booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_FAST_INFANTRY_MOVE))
              .thenReturn(enabled);
        when(mockOptions.booleanOption(OptionsConstants.BASE_SKIP_INELIGIBLE_MOVEMENT))
              .thenReturn(true);
        // Mock planetary conditions for gravity/weather MP calculations
        PlanetaryConditions conditions = new PlanetaryConditions();
        when(mockGame.getPlanetaryConditions()).thenReturn(conditions);
        return mockGame;
    }

    @Nested
    @DisplayName("Run MP Calculation")
    class RunMPTests {

        @Test
        @DisplayName("0 MP infantry gets 2 run MP with fast movement enabled")
        void zeroMPInfantryGets2RunMP() {
            Infantry infantry = createInfantry(0);
            infantry.setGame(createGameWithFastMovement(true));

            assertEquals(0, infantry.getWalkMP());
            assertEquals(2, infantry.getRunMP());
        }

        @Test
        @DisplayName("0 MP infantry gets 0 run MP with fast movement disabled")
        void zeroMPInfantryGets0RunMPWhenDisabled() {
            Infantry infantry = createInfantry(0);
            infantry.setGame(createGameWithFastMovement(false));

            assertEquals(0, infantry.getWalkMP());
            assertEquals(0, infantry.getRunMP());
        }

        @Test
        @DisplayName("1 MP infantry gets 2 run MP with fast movement enabled")
        void oneMPInfantryGets2RunMP() {
            Infantry infantry = createInfantry(1);
            infantry.setGame(createGameWithFastMovement(true));

            assertEquals(1, infantry.getWalkMP());
            assertEquals(2, infantry.getRunMP());
        }

        @Test
        @DisplayName("1 MP infantry gets 1 run MP with fast movement disabled")
        void oneMPInfantryGets1RunMPWhenDisabled() {
            Infantry infantry = createInfantry(1);
            infantry.setGame(createGameWithFastMovement(false));

            assertEquals(1, infantry.getWalkMP());
            assertEquals(1, infantry.getRunMP());
        }
    }

    @Nested
    @DisplayName("0 MP Infantry Following-Turn Exhaustion (TO:AR p.25)")
    class ExhaustionTests {

        @Test
        @DisplayName("0 MP infantry that fast-moved is not eligible for movement next turn")
        void exhaustedInfantryCannotMove() {
            Infantry infantry = createInfantry(0);
            infantry.setGame(createGameWithFastMovement(true));
            infantry.movedLastRound = EntityMovementType.MOVE_RUN;

            assertFalse(infantry.isEligibleForMovement());
        }

        @Test
        @DisplayName("0 MP infantry that fast-moved is not eligible for firing next turn")
        void exhaustedInfantryCannotFire() {
            Infantry infantry = createInfantry(0);
            infantry.setGame(createGameWithFastMovement(true));
            infantry.movedLastRound = EntityMovementType.MOVE_RUN;

            assertFalse(infantry.isEligibleForFiring());
        }

        @Test
        @DisplayName("0 MP infantry that walked (1 free hex) is still eligible next turn")
        void walkedInfantryNotExhausted() {
            Infantry infantry = createInfantry(0);
            infantry.setGame(createGameWithFastMovement(true));
            infantry.movedLastRound = EntityMovementType.MOVE_WALK;

            assertTrue(infantry.isEligibleForMovement());
        }

        @Test
        @DisplayName("0 MP infantry that did not move is still eligible next turn")
        void stationaryInfantryNotExhausted() {
            Infantry infantry = createInfantry(0);
            infantry.setGame(createGameWithFastMovement(true));
            infantry.movedLastRound = EntityMovementType.MOVE_NONE;

            assertTrue(infantry.isEligibleForMovement());
        }

        @Test
        @DisplayName("Non-0-MP infantry is NOT exhausted after fast movement")
        void nonZeroMPInfantryNotExhausted() {
            Infantry infantry = createInfantry(1);
            infantry.setGame(createGameWithFastMovement(true));
            infantry.movedLastRound = EntityMovementType.MOVE_RUN;

            assertTrue(infantry.isEligibleForMovement());
            assertTrue(infantry.isEligibleForFiring());
        }

        @Test
        @DisplayName("Exhaustion does not apply when fast movement option is disabled")
        void exhaustionDisabledWhenOptionOff() {
            Infantry infantry = createInfantry(0);
            infantry.setGame(createGameWithFastMovement(false));
            infantry.movedLastRound = EntityMovementType.MOVE_RUN;

            assertTrue(infantry.isEligibleForMovement());
        }

        @Test
        @DisplayName("Exhaustion clears after newRound() transition (moved -> movedLastRound -> MOVE_NONE)")
        void exhaustionClearsAfterNewRound() {
            Infantry infantry = createInfantry(0);
            infantry.setGame(createGameWithFastMovement(true));

            // Simulate: unit fast-moved this round
            infantry.moved = EntityMovementType.MOVE_RUN;

            // Round transition: moved -> movedLastRound, moved reset to MOVE_NONE
            infantry.newRound(2);

            // Now movedLastRound == MOVE_RUN, so unit should be exhausted
            assertFalse(infantry.isEligibleForMovement(),
                  "Should be exhausted the round after fast movement");
            assertFalse(infantry.isEligibleForFiring(),
                  "Should be exhausted the round after fast movement");

            // Another round transition: movedLastRound becomes MOVE_NONE (since unit couldn't move)
            infantry.newRound(3);

            // Exhaustion should now be cleared
            assertTrue(infantry.isEligibleForMovement(),
                  "Exhaustion should clear after one round of being unable to act");
            assertTrue(infantry.isEligibleForFiring(),
                  "Exhaustion should clear after one round of being unable to act");
        }
    }
}

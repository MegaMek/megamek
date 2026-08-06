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

package megamek.server.totalWarfare;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import megamek.common.compute.Compute;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.rolls.Roll;
import megamek.common.units.Crew;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Tank;
import megamek.common.units.VTOL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

/**
 * Tests for the vehicle Edge critical-hit triggers.
 * <ul>
 *     <li>{@code EDGE_WHEN_TANK_DESTROYED} - catastrophic results (crew killed, ammo, fuel, engine, rotor blown off)</li>
 *     <li>{@code EDGE_WHEN_TANK_TURRET_BLOWN_OFF} - a turret blown off result</li>
 *     <li>{@code EDGE_WHEN_TANK_MOTIVE_CRIT} - an immobilizing motive system damage result</li>
 * </ul>
 * The first two are validated through the {@link TWGameManager#tankShouldUseEdgeForCrit} decision logic; the motive
 * trigger is validated end-to-end through {@link TWGameManager#vehicleMotiveDamage} with the dice mocked.
 */
class VehicleEdgeTriggerTest {

    private final TWGameManager gameManager = new TWGameManager();

    @BeforeEach
    void setUp() {
        gameManager.getGame().setOptions(new GameOptions());
    }

    /**
     * Builds a mocked vehicle whose enabled Edge triggers are controlled directly by stubbing
     * {@link Tank#shouldUseEdge(String)}, isolating the decision logic from game options and crew Edge state.
     */
    private Tank vehicleWithTriggers(boolean destroyedTrigger, boolean turretTrigger) {
        Tank tank = mock(Tank.class);
        lenient().when(tank.shouldUseEdge(OptionsConstants.EDGE_WHEN_TANK_DESTROYED)).thenReturn(destroyedTrigger);
        lenient().when(tank.shouldUseEdge(OptionsConstants.EDGE_WHEN_TANK_TURRET_BLOWN_OFF)).thenReturn(turretTrigger);
        return tank;
    }

    @Nested
    @DisplayName("Catastrophic trigger (EDGE_WHEN_TANK_DESTROYED)")
    class CatastrophicTriggerTests {

        @ParameterizedTest(name = "critType={0} triggers the catastrophic reroll")
        @ValueSource(ints = { Tank.CRIT_CREW_KILLED, Tank.CRIT_AMMO, Tank.CRIT_FUEL_TANK, Tank.CRIT_ENGINE,
                              VTOL.CRIT_ROTOR_DESTROYED })
        void catastrophicResultsUseEdgeWhenEnabled(int critType) {
            Tank tank = vehicleWithTriggers(true, false);
            assertTrue(gameManager.tankShouldUseEdgeForCrit(tank, critType),
                  "Catastrophic result should spend Edge when the catastrophic trigger is enabled");
        }

        @ParameterizedTest(name = "critType={0} does not trigger when the option is off")
        @ValueSource(ints = { Tank.CRIT_CREW_KILLED, Tank.CRIT_AMMO, Tank.CRIT_FUEL_TANK, Tank.CRIT_ENGINE,
                              VTOL.CRIT_ROTOR_DESTROYED })
        void catastrophicResultsSkipEdgeWhenDisabled(int critType) {
            Tank tank = vehicleWithTriggers(false, true);
            assertFalse(gameManager.tankShouldUseEdgeForCrit(tank, critType),
                  "Catastrophic result should not spend Edge when the catastrophic trigger is disabled");
        }

        @Test
        @DisplayName("Rotor blown off is treated as catastrophic")
        void rotorBlownOffIsCatastrophic() {
            Tank tank = vehicleWithTriggers(true, false);
            assertTrue(gameManager.tankShouldUseEdgeForCrit(tank, VTOL.CRIT_ROTOR_DESTROYED),
                  "A blown-off rotor forces a crash and should use the catastrophic trigger");
        }
    }

    @Nested
    @DisplayName("Turret blown off trigger (EDGE_WHEN_TANK_TURRET_BLOWN_OFF)")
    class TurretTriggerTests {

        @Test
        @DisplayName("Turret destroyed uses its own trigger")
        void turretDestroyedUsesTurretTrigger() {
            Tank tank = vehicleWithTriggers(false, true);
            assertTrue(gameManager.tankShouldUseEdgeForCrit(tank, Tank.CRIT_TURRET_DESTROYED),
                  "Turret blown off should spend Edge when the turret trigger is enabled");
        }

        @Test
        @DisplayName("Turret destroyed is not covered by the catastrophic trigger")
        void turretDestroyedNotCoveredByCatastrophic() {
            Tank tank = vehicleWithTriggers(true, false);
            assertFalse(gameManager.tankShouldUseEdgeForCrit(tank, Tank.CRIT_TURRET_DESTROYED),
                  "Turret blown off is its own trigger and must not fire from the catastrophic option");
        }
    }

    @Nested
    @DisplayName("Non-triggering results")
    class NonTriggeringResultTests {

        @Test
        @DisplayName("No critical never spends Edge")
        void critNoneNeverTriggers() {
            Tank tank = vehicleWithTriggers(true, true);
            assertFalse(gameManager.tankShouldUseEdgeForCrit(tank, Tank.CRIT_NONE),
                  "CRIT_NONE should never spend Edge, even with every trigger enabled");
        }

        @ParameterizedTest(name = "non-catastrophic critType={0} never spends Edge")
        @ValueSource(ints = { Tank.CRIT_SENSOR, Tank.CRIT_STABILIZER, Tank.CRIT_WEAPON_DESTROYED, Tank.CRIT_TURRET_JAM,
                              Tank.CRIT_TURRET_LOCK, Tank.CRIT_DRIVER, Tank.CRIT_COMMANDER, Tank.CRIT_CREW_STUNNED,
                              Tank.CRIT_CARGO })
        void minorResultsNeverTrigger(int critType) {
            Tank tank = vehicleWithTriggers(true, true);
            assertFalse(gameManager.tankShouldUseEdgeForCrit(tank, critType),
                  "Non-catastrophic, non-turret results should never spend Edge");
        }
    }

    @Nested
    @DisplayName("Immobilizing motive damage trigger (EDGE_WHEN_TANK_MOTIVE_CRIT)")
    class MotiveDamageEdgeTests {

        /**
         * Builds a tracked vehicle (so {@code vehicleMotiveDamage} actually rolls, unlike a VTOL) with a mocked crew
         * that has Edge available and the motive trigger set as requested.
         */
        private Tank trackedVehicle(boolean motiveTrigger) {
            Tank tank = mock(Tank.class);
            when(tank.getMovementMode()).thenReturn(EntityMovementMode.TRACKED);
            lenient().when(tank.shouldUseEdge(OptionsConstants.EDGE_WHEN_TANK_MOTIVE_CRIT)).thenReturn(motiveTrigger);

            Crew crew = mock(Crew.class);
            PilotOptions crewOptions = mock(PilotOptions.class);
            lenient().when(crewOptions.intOption(OptionsConstants.EDGE)).thenReturn(1);
            lenient().when(crew.getOptions()).thenReturn(crewOptions);
            lenient().when(tank.getCrew()).thenReturn(crew);
            return tank;
        }

        private Roll rollOf(int value) {
            Roll roll = mock(Roll.class);
            when(roll.getIntValue()).thenReturn(value);
            lenient().when(roll.getReport()).thenReturn(String.valueOf(value));
            return roll;
        }

        @Test
        @DisplayName("An immobilizing (12) motive roll is rerolled, spending one Edge")
        void immobilizingRollSpendsEdgeOnReroll() {
            Tank tank = trackedVehicle(true);
            Crew crew = tank.getCrew();
            // First roll immobilizes (12 = Major Damage); the Edge reroll comes up harmless (4).
            Roll immobilizing = rollOf(12);
            Roll harmless = rollOf(4);

            try (MockedStatic<Compute> mockedCompute = mockStatic(Compute.class)) {
                mockedCompute.when(() -> Compute.rollD6(2)).thenReturn(immobilizing, harmless);
                gameManager.vehicleMotiveDamage(tank, 0);
            }

            verify(crew, times(1)).decreaseEdge();
        }

        @Test
        @DisplayName("An immobilizing roll is not rerolled when the trigger is disabled")
        void immobilizingRollKeepsResultWhenTriggerOff() {
            Tank tank = trackedVehicle(false);
            Crew crew = tank.getCrew();
            Roll immobilizing = rollOf(12);

            try (MockedStatic<Compute> mockedCompute = mockStatic(Compute.class)) {
                mockedCompute.when(() -> Compute.rollD6(2)).thenReturn(immobilizing);
                gameManager.vehicleMotiveDamage(tank, 0);
            }

            verify(crew, never()).decreaseEdge();
        }

        @Test
        @DisplayName("A non-immobilizing motive roll does not spend Edge")
        void nonImmobilizingRollDoesNotSpendEdge() {
            Tank tank = trackedVehicle(true);
            Crew crew = tank.getCrew();
            // Moderate damage (8) is not the Major Damage / immobilizing result.
            Roll moderate = rollOf(8);

            try (MockedStatic<Compute> mockedCompute = mockStatic(Compute.class)) {
                mockedCompute.when(() -> Compute.rollD6(2)).thenReturn(moderate);
                gameManager.vehicleMotiveDamage(tank, 0);
            }

            verify(crew, never()).decreaseEdge();
        }
    }
}

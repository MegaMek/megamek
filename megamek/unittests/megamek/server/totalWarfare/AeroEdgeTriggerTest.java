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

package megamek.server.totalWarfare;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import megamek.common.options.OptionsConstants;
import megamek.common.units.Aero;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for the aerospace catastrophic Edge critical-hit trigger decision logic
 * ({@link TWGameManager#aeroShouldUseEdgeForCrit}).
 * <p>
 * The {@code EDGE_WHEN_AERO_CATASTROPHIC} trigger rerolls the aerospace critical hit table roll on results that can
 * destroy the unit: crew, engine, and fuel tank hits.
 */
class AeroEdgeTriggerTest {

    private final TWGameManager gameManager = new TWGameManager();

    /**
     * Builds a mocked aerospace unit whose catastrophic Edge trigger is controlled directly by stubbing
     * {@link Aero#shouldUseEdge(String)}, isolating the decision logic from game options and crew Edge state.
     */
    private Aero aeroWithTrigger(boolean catastrophicTrigger) {
        Aero aero = mock(Aero.class);
        lenient().when(aero.shouldUseEdge(OptionsConstants.EDGE_WHEN_AERO_CATASTROPHIC))
              .thenReturn(catastrophicTrigger);
        return aero;
    }

    @Nested
    @DisplayName("Catastrophic trigger (EDGE_WHEN_AERO_CATASTROPHIC)")
    class CatastrophicTriggerTests {

        @ParameterizedTest(name = "critType={0} triggers the catastrophic reroll")
        @ValueSource(ints = { Aero.CRIT_CREW, Aero.CRIT_ENGINE, Aero.CRIT_FUEL_TANK })
        void catastrophicResultsUseEdgeWhenEnabled(int critType) {
            Aero aero = aeroWithTrigger(true);
            assertTrue(gameManager.aeroShouldUseEdgeForCrit(aero, critType),
                  "A unit-destroying critical should spend Edge when the catastrophic trigger is enabled");
        }

        @ParameterizedTest(name = "critType={0} does not trigger when the option is off")
        @ValueSource(ints = { Aero.CRIT_CREW, Aero.CRIT_ENGINE, Aero.CRIT_FUEL_TANK })
        void catastrophicResultsSkipEdgeWhenDisabled(int critType) {
            Aero aero = aeroWithTrigger(false);
            assertFalse(gameManager.aeroShouldUseEdgeForCrit(aero, critType),
                  "A unit-destroying critical should not spend Edge when the catastrophic trigger is disabled");
        }
    }

    @Nested
    @DisplayName("Non-triggering results")
    class NonTriggeringResultTests {

        @Test
        @DisplayName("No critical never spends Edge")
        void critNoneNeverTriggers() {
            Aero aero = aeroWithTrigger(true);
            assertFalse(gameManager.aeroShouldUseEdgeForCrit(aero, Aero.CRIT_NONE),
                  "CRIT_NONE should never spend Edge, even with the trigger enabled");
        }

        @ParameterizedTest(name = "non-catastrophic critType={0} never spends Edge")
        @ValueSource(ints = { Aero.CRIT_FCS, Aero.CRIT_WEAPON, Aero.CRIT_CONTROL, Aero.CRIT_SENSOR, Aero.CRIT_BOMB,
                              Aero.CRIT_AVIONICS, Aero.CRIT_GEAR, Aero.CRIT_HEATSINK, Aero.CRIT_CARGO, Aero.CRIT_DOOR,
                              Aero.CRIT_LIFE_SUPPORT, Aero.CRIT_LEFT_THRUSTER, Aero.CRIT_RIGHT_THRUSTER })
        void minorResultsNeverTrigger(int critType) {
            Aero aero = aeroWithTrigger(true);
            assertFalse(gameManager.aeroShouldUseEdgeForCrit(aero, critType),
                  "Non-catastrophic results should never spend Edge");
        }
    }
}

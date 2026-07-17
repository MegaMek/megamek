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

package megamek.common.weapons.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Vector;

import megamek.common.Report;
import megamek.common.compute.Compute;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.rolls.Roll;
import megamek.common.units.Crew;
import megamek.common.units.Entity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Tests for the RISC equipment malfunction Edge reroll ({@link EnergyWeaponHandler#rerollRiscMalfunctionWithEdge} and
 * {@link EnergyWeaponHandler#riscStillMalfunctions}), shared by the RISC Laser Pulse Module (explodes on a 2) and the
 * RISC Hyper Laser (explodes on a 3 or less). A malfunction may be rerolled with Edge, but only once.
 */
class EnergyWeaponHandlerRiscEdgeTest {

    private static final int PULSE_MODULE_THRESHOLD = 2;
    private static final int HYPER_LASER_THRESHOLD = 3;

    private Roll rollOf(int value) {
        Roll roll = mock(Roll.class);
        when(roll.getIntValue()).thenReturn(value);
        lenient().when(roll.getReport()).thenReturn(String.valueOf(value));
        return roll;
    }

    private Entity attacker(boolean riscTrigger) {
        Entity entity = mock(Entity.class);
        Crew crew = mock(Crew.class);
        PilotOptions crewOptions = mock(PilotOptions.class);
        lenient().when(crewOptions.intOption(OptionsConstants.EDGE)).thenReturn(1);
        lenient().when(crew.getOptions()).thenReturn(crewOptions);
        lenient().when(entity.getCrew()).thenReturn(crew);
        lenient().when(entity.shouldUseEdge(OptionsConstants.EDGE_WHEN_RISC_FAIL)).thenReturn(riscTrigger);
        return entity;
    }

    @Test
    @DisplayName("The reroll value is returned and one Edge is spent")
    void rerollReturnsValueAndSpendsEdge() {
        Entity attacker = attacker(true);
        Vector<Report> reports = new Vector<>();
        Roll reroll = rollOf(9);

        int result;
        try (MockedStatic<Compute> mockedCompute = mockStatic(Compute.class)) {
            mockedCompute.when(() -> Compute.rollD6(2)).thenReturn(reroll);
            result = EnergyWeaponHandler.rerollRiscMalfunctionWithEdge(attacker, 1, reports);
        }

        assertEquals(9, result, "The rerolled value should be returned for the caller to evaluate");
        verify(attacker.getCrew(), times(1)).decreaseEdge();
        assertFalse(reports.isEmpty(), "Edge reports should be added");
    }

    @Test
    @DisplayName("A reroll that still malfunctions returns its value and spends exactly one Edge")
    void rerollThatStillMalfunctionsSpendsOneEdge() {
        Entity attacker = attacker(true);
        Vector<Report> reports = new Vector<>();
        Roll reroll = rollOf(2);

        int result;
        try (MockedStatic<Compute> mockedCompute = mockStatic(Compute.class)) {
            mockedCompute.when(() -> Compute.rollD6(2)).thenReturn(reroll);
            result = EnergyWeaponHandler.rerollRiscMalfunctionWithEdge(attacker, 1, reports);
        }

        assertEquals(2, result, "The rerolled value is returned even when it still malfunctions");
        assertTrue(EnergyWeaponHandler.riscStillMalfunctions(result, PULSE_MODULE_THRESHOLD),
              "A reroll of 2 still explodes a pulse module");
        verify(attacker.getCrew(), times(1)).decreaseEdge();
    }

    @Test
    @DisplayName("With the trigger disabled, Edge is not used and -1 is returned")
    void triggerDisabledDoesNotUseEdge() {
        Entity attacker = attacker(false);
        Vector<Report> reports = new Vector<>();

        int result = EnergyWeaponHandler.rerollRiscMalfunctionWithEdge(attacker, 1, reports);

        assertEquals(-1, result, "Without the trigger no reroll happens (-1 sentinel)");
        verify(attacker.getCrew(), never()).decreaseEdge();
        assertTrue(reports.isEmpty(), "No reports should be added when the trigger is disabled");
    }

    @Test
    @DisplayName("Without Edge (-1) the malfunction always stands")
    void noEdgeAlwaysMalfunctions() {
        assertTrue(EnergyWeaponHandler.riscStillMalfunctions(-1, PULSE_MODULE_THRESHOLD));
        assertTrue(EnergyWeaponHandler.riscStillMalfunctions(-1, HYPER_LASER_THRESHOLD));
    }

    @Test
    @DisplayName("Pulse Module (threshold 2) explodes again only on a reroll of 2")
    void pulseModuleThreshold() {
        assertTrue(EnergyWeaponHandler.riscStillMalfunctions(2, PULSE_MODULE_THRESHOLD),
              "A reroll of 2 still explodes");
        assertFalse(EnergyWeaponHandler.riscStillMalfunctions(3, PULSE_MODULE_THRESHOLD),
              "A reroll of 3 clears the pulse module explosion");
        assertFalse(EnergyWeaponHandler.riscStillMalfunctions(11, PULSE_MODULE_THRESHOLD),
              "A high reroll clears the pulse module explosion");
    }

    @Test
    @DisplayName("Hyper Laser (threshold 3) explodes again on a reroll of 3 or less")
    void hyperLaserThreshold() {
        assertTrue(EnergyWeaponHandler.riscStillMalfunctions(2, HYPER_LASER_THRESHOLD), "A reroll of 2 still explodes");
        assertTrue(EnergyWeaponHandler.riscStillMalfunctions(3, HYPER_LASER_THRESHOLD), "A reroll of 3 still explodes");
        assertFalse(EnergyWeaponHandler.riscStillMalfunctions(4, HYPER_LASER_THRESHOLD),
              "A reroll of 4 clears the hyper laser explosion");
    }
}

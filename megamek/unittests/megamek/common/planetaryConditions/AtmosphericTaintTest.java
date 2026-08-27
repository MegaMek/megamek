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

package megamek.common.planetaryConditions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests the seven atmospheric taint settings of TO:AR p.54 and the questions the rules ask of them.
 */
class AtmosphericTaintTest {

    @Test
    @DisplayName("Breathable air is neither tainted nor toxic and belongs to no taint category")
    void breathableAirHasNoTaint() {
        AtmosphericTaint breathable = AtmosphericTaint.BREATHABLE;

        assertTrue(breathable.isBreathable());
        assertFalse(breathable.isTainted());
        assertFalse(breathable.isToxic());
        assertFalse(breathable.isTaintedOrToxic());
        assertFalse(breathable.isCaustic());
        assertFalse(breathable.isRadiological());
        assertFalse(breathable.isFlammable());
    }

    @Test
    @DisplayName("Every fouled atmosphere is tainted or toxic, and never both")
    void everyFouledAtmosphereHasExactlyOneSeverity() {
        for (AtmosphericTaint atmosphericTaint : AtmosphericTaint.values()) {
            if (atmosphericTaint.isBreathable()) {
                continue;
            }
            assertTrue(atmosphericTaint.isTaintedOrToxic(), atmosphericTaint + " should count as fouled air");
            assertTrue(atmosphericTaint.isTainted() != atmosphericTaint.isToxic(),
                  atmosphericTaint + " should be tainted or toxic but not both");
        }
    }

    @Test
    @DisplayName("Every fouled atmosphere belongs to exactly one taint category")
    void everyFouledAtmosphereHasExactlyOneCategory() {
        for (AtmosphericTaint atmosphericTaint : AtmosphericTaint.values()) {
            if (atmosphericTaint.isBreathable()) {
                continue;
            }
            int categoryCount = (atmosphericTaint.isCaustic() ? 1 : 0)
                  + (atmosphericTaint.isRadiological() ? 1 : 0)
                  + (atmosphericTaint.isFlammable() ? 1 : 0);
            assertEquals(1, categoryCount, atmosphericTaint + " should belong to exactly one taint category");
        }
    }

    @Test
    @DisplayName("A flammable atmosphere makes fires easier to start, by 2 when tainted and 4 when toxic")
    void flammableAtmosphereEasesIgnition() {
        // Ignition succeeds on a roll at or above the target, so an easier fire is a negative modifier.
        assertEquals(-2, AtmosphericTaint.FLAMMABLE_TAINTED.getIgniteModifier());
        assertEquals(-4, AtmosphericTaint.FLAMMABLE_TOXIC.getIgniteModifier());
    }

    @Test
    @DisplayName("An atmosphere that is not flammable does not change ignition rolls")
    void otherAtmospheresDoNotChangeIgnition() {
        assertEquals(0, AtmosphericTaint.BREATHABLE.getIgniteModifier());
        assertEquals(0, AtmosphericTaint.CAUSTIC_TAINTED.getIgniteModifier());
        assertEquals(0, AtmosphericTaint.CAUSTIC_TOXIC.getIgniteModifier());
        assertEquals(0, AtmosphericTaint.RADIOLOGICAL_TAINTED.getIgniteModifier());
        assertEquals(0, AtmosphericTaint.RADIOLOGICAL_TOXIC.getIgniteModifier());
    }

    @Test
    @DisplayName("An atmosphere round-trips through its external id")
    void externalIdRoundTrips() {
        for (AtmosphericTaint atmosphericTaint : AtmosphericTaint.values()) {
            assertEquals(atmosphericTaint,
                  AtmosphericTaint.getAtmosphericTaint(atmosphericTaint.getExternalId()),
                  atmosphericTaint + " should be recovered from its own external id");
        }
    }

    @Test
    @DisplayName("An unknown external id falls back to breathable air rather than failing")
    void unknownExternalIdFallsBackToBreathable() {
        assertEquals(AtmosphericTaint.BREATHABLE, AtmosphericTaint.getAtmosphericTaint("NOT_A_REAL_TAINT"));
    }
}

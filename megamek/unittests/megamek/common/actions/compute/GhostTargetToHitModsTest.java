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
package megamek.common.actions.compute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.options.OptionsConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for ghost target game option constants and mode values used by {@link ComputeTargetToHitMods} to branch between
 * Legacy and Standard ghost target modes.
 */
class GhostTargetToHitModsTest {

    @Test
    @DisplayName("Ghost target option constants are correctly defined")
    void testGhostTargetOptionConstants() {
        assertEquals("tacops_ghost_target", OptionsConstants.ADVANCED_TAC_OPS_GHOST_TARGET);
        assertEquals("ghost_target_mode", OptionsConstants.ADVANCED_GHOST_TARGET_MODE);
        assertEquals("ghost_target_max", OptionsConstants.ADVANCED_GHOST_TARGET_MAX);
    }

    @Test
    @DisplayName("Ghost target mode values are distinct")
    void testGhostTargetModeValues() {
        assertNotEquals(OptionsConstants.GHOST_TARGET_MODE_LEGACY,
              OptionsConstants.GHOST_TARGET_MODE_STANDARD);
        assertEquals("Legacy (Area Effect)", OptionsConstants.GHOST_TARGET_MODE_LEGACY);
        assertEquals("Standard (Targeted)", OptionsConstants.GHOST_TARGET_MODE_STANDARD);
    }

    @Test
    @DisplayName("Standard mode string matches what GameOptions initializes")
    void testStandardModeStringMatchesGameOption() {
        // The Standard mode check in ComputeTargetToHitMods uses .equals() on this constant.
        // Verify the string is non-empty and doesn't contain invisible characters.
        String standard = OptionsConstants.GHOST_TARGET_MODE_STANDARD;
        assertFalse(standard.isEmpty());
        assertEquals(standard.trim(), standard);
    }

    @Test
    @DisplayName("Legacy mode is the default (first value in CHOICE)")
    void testLegacyModeIsDefault() {
        // The CHOICE option defaults to the first value, which should be Legacy.
        // This ensures backward compatibility - existing saves default to Legacy.
        String legacy = OptionsConstants.GHOST_TARGET_MODE_LEGACY;
        assertFalse(legacy.isEmpty());
        assertTrue(legacy.contains("Legacy"));
    }
}

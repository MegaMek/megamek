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
 */
package megamek.common.rules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.rules.core.CoreRulesHeat;
import megamek.common.rules.totalwarfare.TWRulesHeat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RulesHeat rules variants")
class RulesHeatTest {

    @Test
    @DisplayName("core and total warfare variants construct and inherit from the base rules type")
    void coreAndTotalWarfareVariantsConstructAndInherit() {
        CoreRulesHeat core = new CoreRulesHeat();
        TWRulesHeat totalWarfare = new TWRulesHeat();

        assertNotNull(core, "Core rules implementation should be constructible.");
        assertNotNull(totalWarfare, "Total warfare rules implementation should be constructible.");
        assertInstanceOf(RulesHeat.class, core, "Core rules should extend the base rules type.");
        assertInstanceOf(RulesHeat.class, totalWarfare, "Total warfare rules should extend the base rules type.");
    }

    @Test
    @DisplayName("total warfare follows the Avoiding Shutdown option")
    void totalWarfareFollowsTheAvoidingShutdownOption() {
        TWRulesHeat totalWarfare = new TWRulesHeat();

        assertTrue(totalWarfare.usesAvoidingShutdown(true), "The TacOps rule applies when its option is on.");
        assertFalse(totalWarfare.usesAvoidingShutdown(false), "Off by default, the plain Avoid number applies.");
    }

    @Test
    @DisplayName("core rules also follow the Avoiding Shutdown option")
    void coreRulesAlsoFollowTheAvoidingShutdownOption() {
        // The Core Rules have no such rule of their own (Core p.104), but the TacOps option can be played on top.
        CoreRulesHeat core = new CoreRulesHeat();

        assertTrue(core.usesAvoidingShutdown(true), "The TacOps rule applies when its option is on.");
        assertFalse(core.usesAvoidingShutdown(false), "Off by default, the plain Avoid number applies.");
    }
}

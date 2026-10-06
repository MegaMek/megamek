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

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import megamek.common.rules.core.CoreRulesEnvironment;
import megamek.common.rules.totalwarfare.TWRulesEnvironment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Simple construct-and-inherit checks for environment rule implementations. Kept concise but adopting the
 * documented test layout used elsewhere: descriptive DisplayNames and clear messages for assertions.
 */
@DisplayName("RulesEnvironment rules variants")
class RulesEnvironmentTest {

    @Test
    @DisplayName("core and total warfare environment rule instances are constructible")
    void coreAndTotalWarfareEnvironmentRulesAreConstructible() {
        CoreRulesEnvironment core = new CoreRulesEnvironment();
        TWRulesEnvironment totalWarfare = new TWRulesEnvironment();

        assertNotNull(core, "Core rules implementation should be constructible.");
        assertNotNull(totalWarfare, "Total warfare rules implementation should be constructible.");
        assertInstanceOf(RulesEnvironment.class, core, "Core rules should extend the base rules type.");
        assertInstanceOf(RulesEnvironment.class, totalWarfare, "Total warfare rules should extend the base rules type.");
    }
}

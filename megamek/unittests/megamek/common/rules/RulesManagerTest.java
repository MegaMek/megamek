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

import megamek.common.rules.core.CoreRulesManager;
import megamek.common.rules.core.CoreRulesTarget;
import megamek.common.rules.totalwarfare.TWRulesManager;
import megamek.common.rules.totalwarfare.TWRulesTarget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RulesManager rules variants")
class RulesManagerTest {

    @Test
    @DisplayName("core and total warfare managers expose the expected rule implementations")
    void coreAndTotalWarfareManagersExposeRuleImplementations() {
        CoreRulesManager core = new CoreRulesManager();
        TWRulesManager totalWarfare = new TWRulesManager();

        assertNotNull(core, "Core rules implementation should be constructible.");
        assertNotNull(totalWarfare, "Total warfare rules implementation should be constructible.");
        assertInstanceOf(RulesManager.class, core, "Core rules should extend the base rules type.");
        assertInstanceOf(RulesManager.class, totalWarfare, "Total warfare rules should extend the base rules type.");
        assertInstanceOf(CoreRulesTarget.class, core.getRulesTarget(), "Core manager should expose core targeting rules.");
        assertInstanceOf(TWRulesTarget.class, totalWarfare.getRulesTarget(), "Total warfare manager should expose total warfare targeting rules.");
        assertNotNull(core.getRulesAmmo(), "Core manager should provide ammo rules.");
        assertNotNull(totalWarfare.getRulesAmmo(), "Total warfare manager should provide ammo rules.");
        assertNotNull(core.getRulesTerrain(), "Core manager should provide terrain rules.");
        assertNotNull(totalWarfare.getRulesTerrain(), "Total warfare manager should provide terrain rules.");
        assertNotNull(core.getRulesUnderwater(), "Core manager should provide underwater rules.");
        assertNotNull(totalWarfare.getRulesUnderwater(), "Total warfare manager should provide underwater rules.");
    }
}

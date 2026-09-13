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

import megamek.common.rules.core.CoreRulesArmor;
import megamek.common.rules.totalwarfare.TWRulesArmor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RulesArmor rules variants")
class RulesArmorTest {

    @Test
    @DisplayName("core and total warfare variants construct and inherit from the base rules type")
    void coreAndTotalWarfareVariantsConstructAndInherit() {
        CoreRulesArmor core = new CoreRulesArmor();
        TWRulesArmor totalWarfare = new TWRulesArmor();

        assertNotNull(core, "Core rules implementation should be constructible.");
        assertNotNull(totalWarfare, "Total warfare rules implementation should be constructible.");
        assertInstanceOf(RulesArmor.class, core, "Core rules should extend the base rules type.");
        assertInstanceOf(RulesArmor.class, totalWarfare, "Total warfare rules should extend the base rules type.");
    }
}

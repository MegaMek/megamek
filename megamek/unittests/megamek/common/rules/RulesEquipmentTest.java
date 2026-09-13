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

import megamek.common.rules.core.CoreRulesEquipment;
import megamek.common.rules.totalwarfare.TWRulesEquipment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RulesEquipment rules variants")
class RulesEquipmentTest {

    @Test
    @DisplayName("core and total warfare variants construct and inherit from the base rules type")
    void coreAndTotalWarfareVariantsConstructAndInherit() {
        CoreRulesEquipment core = new CoreRulesEquipment();
        TWRulesEquipment totalWarfare = new TWRulesEquipment();

        assertNotNull(core, "Core rules implementation should be constructible.");
        assertNotNull(totalWarfare, "Total warfare rules implementation should be constructible.");
        assertInstanceOf(RulesEquipment.class, core, "Core rules should extend the base rules type.");
        assertInstanceOf(RulesEquipment.class, totalWarfare, "Total warfare rules should extend the base rules type.");
    }
}

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
/**
 * Tests for unit-specific rule differences between Core and Total Warfare rule sets.
 *
 * Updated to follow the documented, example-driven style used in the planetary condition tests:
 * helper-focused fixtures, clear DisplayName annotations and explicit assertion messages so test
 * failures are self-explanatory.
 */
package megamek.common.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.rules.core.CoreRulesUnits;
import megamek.common.rules.totalwarfare.TWRulesUnits;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RulesUnits rules variants")
class RulesUnitsTest {

    /**
     * Verify the key behavioral divergences between Core and Total Warfare unit rules.
     */
    @Test
    @DisplayName("core and total warfare unit rules apply different leg and MP logic")
    void coreAndTotalWarfareUnitRulesApplyDifferentLegAndMovementLogic() {
        CoreRulesUnits core = new CoreRulesUnits();
        TWRulesUnits totalWarfare = new TWRulesUnits();

        assertNotNull(core, "Core rules implementation should be constructible.");
        assertNotNull(totalWarfare, "Total warfare rules implementation should be constructible.");
        assertInstanceOf(RulesUnits.class, core, "Core rules should extend the base rules type.");
        assertInstanceOf(RulesUnits.class, totalWarfare, "Total warfare rules should extend the base rules type.");

        // Mule kick differences
        assertEquals(0, core.getMuleKickModifier(), "Core mule kicks do not have a hit modifier.");
        assertEquals(1, totalWarfare.getMuleKickModifier(), "Total Warfare mule kicks add one to hit.");

        // Quad movement reductions for destroyed legs
        assertEquals(5, core.reduceQuadWalkMP(6, 1, 0, 0, false), "Core quad leg loss reduces walk MP by one.");
        assertEquals(1, core.reduceQuadWalkMP(3, 3, 0, 0, false), "Core quad walk MP is reduced to one after three destroyed legs.");
        assertEquals(0, core.reduceQuadWalkMP(3, 4, 0, 0, false), "Core quad walk MP reaches zero with all legs destroyed.");
        assertEquals(1, totalWarfare.reduceQuadWalkMP(6, 2, 0, 0, false), "Total Warfare reduces a quad to one MP with two destroyed legs.");

        // Hip hits and actuator effects
        assertEquals(4, totalWarfare.reduceQuadWalkMP(8, 1, 1, 0, false), "Total Warfare halves MP for a single hip hit after leg loss.");
        assertEquals(8, core.getMekMPReduction(2, true, 10), "Core hip hit reduction is a flat reduction of hip hits.");
        assertEquals(5, totalWarfare.getMekMPReduction(1, false, 10), "Total Warfare halves MP for a single hip hit.");

        // Minimum MP differences
        assertEquals(1, core.getMinimumMP(0), "Core minimum MP is one.");
        assertEquals(0, totalWarfare.getMinimumMP(0), "Total Warfare minimum MP is zero.");

        // Physical phase torso twist behavior
        assertTrue(core.getPhysicalTwistEnabled(), "Core rules allow torso twists in the physical phase.");
        assertFalse(totalWarfare.getPhysicalTwistEnabled(), "Total Warfare does not enable the base RulesUnits torso twist override.");
    }
}

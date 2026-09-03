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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import megamek.common.rules.core.CoreRulesPilot;
import megamek.common.rules.totalwarfare.TWRulesPilot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RulesPilot rules variants")
class RulesPilotTest {

    @Test
    @DisplayName("core and total warfare pilot rules apply different explosion and seatbelt modifiers")
    void coreAndTotalWarfarePilotRulesApplyDistinctSeatbeltLogic() {
        CoreRulesPilot core = new CoreRulesPilot();
        TWRulesPilot totalWarfare = new TWRulesPilot();

        assertNotNull(core, "Core rules implementation should be constructible.");
        assertNotNull(totalWarfare, "Total warfare rules implementation should be constructible.");
        assertInstanceOf(RulesPilot.class, core, "Core rules should extend the base rules type.");
        assertInstanceOf(RulesPilot.class, totalWarfare, "Total warfare rules should extend the base rules type.");

        assertEquals(1, core.getExplosionPilotHits(), "Core explosions cause one pilot hit.");
        assertEquals(2, totalWarfare.getExplosionPilotHits(), "Total Warfare explosions cause two pilot hits.");
        assertEquals(7, core.getSeatbeltGyroModifier(7), "Core gyro checks are not modified.");
        assertEquals(13, totalWarfare.getSeatbeltGyroModifier(7), "Total Warfare gyro checks add six.");
        assertEquals(7, core.getSeatbeltLegModifier(7, 1), "Core legs do not modify seatbelt checks.");
        assertEquals(17, totalWarfare.getSeatbeltLegModifier(7, 2), "Total Warfare two destroyed legs add ten.");
        assertEquals(7, core.getSeatbeltShutdown(7), "Core shutdown seatbelt checks are unchanged.");
        assertEquals(10, totalWarfare.getSeatbeltShutdown(7), "Total Warfare shutdown seatbelt checks are +3.");
    }
}

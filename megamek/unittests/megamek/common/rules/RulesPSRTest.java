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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import megamek.common.rolls.PilotingRollData;
import megamek.common.rules.core.CoreRulesPSR;
import megamek.common.rules.totalwarfare.TWRulesPSR;
import megamek.common.units.EntityMovementType;
import org.junit.jupiter.api.Test;

class RulesPSRTest {

    @Test
    void coreAndTotalWarfareRulesExercisePSRLogic() {
        CoreRulesPSR core = new CoreRulesPSR();
        TWRulesPSR total = new TWRulesPSR();
        ArrayList<PilotingRollData> list = new ArrayList<>();
        list.add(new PilotingRollData(1, 2, "first", 0));
        list.add(new PilotingRollData(2, 1, "second", 0));

        assertNotNull(core);
        assertNotNull(total);
        core.rollRemoveHighest(list);
        total.rollRemoveHighest(list);
        assertTrue(list.size() <= 2);
        assertTrue(core.getHipPenalty() >= 1);
        assertTrue(total.getHipPenalty() >= 1);
        assertTrue(core.getGyroModifier(1, 0) >= 2);
        assertTrue(total.getGyroModifier(1, 0) >= 2);
        assertTrue(core.getLegDestroyedModifier() >= 4);
        assertTrue(total.getLegDestroyedModifier() >= 4);
        assertTrue(core.getSuccessfulDFAModifier() >= 2);
        assertTrue(total.getSuccessfulDFAModifier() >= 2);
        assertTrue(core.getGyroJumpModifier(1, 0) >= 0);
        assertTrue(total.getGyroJumpModifier(1, 0) >= 0);
        assertTrue(core.psrForWaterEntry(EntityMovementType.MOVE_RUN));
        assertTrue(total.psrForWaterEntry(EntityMovementType.MOVE_RUN));
        assertTrue(core.getGyroModifier(0, 0) >= 2);
        assertTrue(total.getGyroModifier(0, 0) >= 2);
    }
}

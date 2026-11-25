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

package megamek.common.battleValue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.client.ui.clientGUI.calculationReport.DummyCalculationReport;
import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;

class ForceBonusOrderBVTest {

    @Test
    void theC3BonusIsTakenFromTheBattleValueIncludingTag() {
        Entity entity = mock(Entity.class);
        // a four-unit C3 network: 20 percent of the BV passed in
        when(entity.getExtraC3BV(anyInt())).thenAnswer(call -> (int) Math.round(call.<Integer>getArgument(0) * 0.2));
        BVCalculator calculator = new BVCalculator(entity) {
            @Override
            public void processTagBonus() {
                adjustedBV += 100;
            }
        };
        calculator.bvReport = new DummyCalculationReport();
        calculator.baseBV = 1000;
        calculator.ignoreSkill = true;

        calculator.adjustBV();

        // TechManual p.315 and Core errata v0.2 p.1: TAG, then C3, then skill
        assertEquals(1320, (int) Math.round(calculator.adjustedBV));
    }
}

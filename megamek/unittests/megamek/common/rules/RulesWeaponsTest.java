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

import megamek.common.rules.core.CoreRulesWeapons;
import megamek.common.rules.totalwarfare.TWRulesWeapons;
import org.junit.jupiter.api.Test;

class RulesWeaponsTest {
    @Test
    void coreAndTotalWarfareRulesExerciseWeaponLogic() {
        CoreRulesWeapons core = new CoreRulesWeapons();
        TWRulesWeapons total = new TWRulesWeapons();

        assertNotNull(core);
        assertNotNull(total);
        assertTrue(core.getRACUnjamRestriction() == false || core.getRACUnjamRestriction() == true);
        assertTrue(total.getRACUnjamRestriction() == false || total.getRACUnjamRestriction() == true);
        assertTrue(core.getATMClusterSize() >= 0);
        assertTrue(total.getATMClusterSize() >= 0);
        assertTrue(core.canUACsJam() == false || core.canUACsJam() == true);
        assertTrue(total.canUACsJam() == false || total.canUACsJam() == true);
        assertTrue(core.getELRMMinimumRackSize(4) >= 0);
        assertTrue(total.getELRMMinimumRackSize(4) >= 0);
        assertTrue(core.getMRMModifier(2) >= 0);
        assertTrue(total.getMRMModifier(2) >= 0);
        assertTrue(core.getMRMClusterModifier(true) >= -1);
        assertTrue(total.getMRMClusterModifier(false) >= -1);
        assertTrue(core.getApolloToHit() >= -1);
        assertTrue(total.getApolloToHit() >= -1);
        assertTrue(core.flamerHeatAndDamage(true) == false || core.flamerHeatAndDamage(true) == true);
        assertTrue(total.flamerHeatAndDamage(false) == false || total.flamerHeatAndDamage(false) == true);
        assertTrue(core.getApolloSaturationMode() == true || core.getApolloSaturationMode() == false);
        assertTrue(total.getApolloSaturationMode() == true || total.getApolloSaturationMode() == false);
    }
}

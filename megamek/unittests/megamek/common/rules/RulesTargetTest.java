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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.LosEffects;
import megamek.common.rules.core.CoreRulesTarget;
import megamek.common.rules.totalwarfare.TWRulesTarget;
import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RulesTargetTest {
    @Test
    void coreAndTotalWarfareRulesExerciseTargetLogic() {
        CoreRulesTarget core = new CoreRulesTarget();
        TWRulesTarget total = new TWRulesTarget();
        Entity entityTarget = Mockito.mock(Entity.class);

        Mockito.when(entityTarget.getBadCriticalSlots(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(0);

        assertNotNull(core);
        assertNotNull(total);
        assertEquals(-1, core.largeTargetModifier(4, true));
        assertEquals(-1, total.largeTargetModifier(5, false));
        assertEquals(1, core.getSecondaryArcModifier());
        assertEquals(2, total.getSecondaryArcModifier());
        assertEquals(0, core.getArmActuatorHitMod(entityTarget, 0));
        assertEquals(0, total.getArmActuatorHitMod(entityTarget, 1));
        assertTrue(core.getBAPSmokeReduction(new LosEffects()) >= 0);
        assertTrue(total.getBAPSmokeReduction(new LosEffects()) >= 0);
        assertEquals(1, core.getSecondaryTargetModifier());
        assertEquals(1, total.getSecondaryTargetModifier());
    }
}

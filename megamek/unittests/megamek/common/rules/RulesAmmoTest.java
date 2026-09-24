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
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.ToHitData;
import megamek.common.equipment.AmmoType;
import megamek.common.rules.core.CoreRulesAmmo;
import megamek.common.rules.totalwarfare.TWRulesAmmo;
import megamek.server.totalWarfare.TWDamageManager;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RulesAmmoTest {
    @Test
    void coreAndTotalWarfareRulesExerciseAmmoLogic() {
        CoreRulesAmmo core = new CoreRulesAmmo();
        TWRulesAmmo total = new TWRulesAmmo();
        AmmoType ammo = Mockito.mock(AmmoType.class);
        Mockito.when(ammo.getRackSize()).thenReturn(4);

        assertEquals(-2, core.armorPiercingMod(ammo));
        assertEquals(-3, total.armorPiercingMod(ammo));

        ToHitData toHit = new ToHitData();
        core.armorPiercingAttackMod(AmmoType.AmmoTypeEnum.AC, toHit, true);
        total.armorPiercingAttackMod(AmmoType.AmmoTypeEnum.AC, toHit, false);
        core.narcHomingTarget(toHit);

        assertEquals(-1, toHit.getValue());
        assertEquals(-1, core.getAXMissileModifier());
        assertEquals(-2, total.getAXMissileModifier());
        assertEquals(5, core.getAXMissileDamage(4, new TWDamageManager.ModsInfo(), 5));
        assertEquals(5, total.getAXMissileDamage(4, new TWDamageManager.ModsInfo(), 5));
        assertEquals(2, core.getSemiGuidedAdjustment(3, true, true));
        assertEquals(0, total.getSemiGuidedAdjustment(3, false, true));
        assertTrue(core.semiGuidedIgnoresCover());
        assertTrue(!total.semiGuidedIgnoresCover());
        assertEquals(2, core.getSemiGuidedNMissiles(true, false));
        assertEquals(0, total.getSemiGuidedNMissiles(true, true));
    }
}

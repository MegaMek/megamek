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

package megamek.common.rules.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.function.Function;

import megamek.common.options.OptionsConstants;
import megamek.common.rules.totalwarfare.TWRulesManager;
import megamek.common.units.Entity;
import megamek.common.units.Tank;
import org.junit.jupiter.api.Test;

/**
 * Issue #9730: every game option the Game Options dialog makes read-only under Core Rules must be one the Core rules
 * classes ignore. Otherwise a value set under Total Warfare, or sent by a MekHQ campaign, would change a Core game while
 * the player cannot change it.
 */
class CoreRulesIgnoredOptionsTest {
    private final CoreRulesManager core = new CoreRulesManager();

    /**
     * For each ignored option, the Core rule that reads it, as a function of the option's value. A Core rule that
     * ignores the option gives the same result for {@code true} and {@code false}.
     */
    private Map<String, Function<Boolean, Object>> coreRulesReadingEachOption() {
        Entity charger = mock(Entity.class);
        when(charger.getWeight()).thenReturn(50.0);
        Entity target = mock(Entity.class);
        int[] turnsPerPlayer = { 3, 2 };
        return Map.of(
              OptionsConstants.BASE_FLAMER_HEAT,
              option -> core.getRulesWeapons().flamerHeatAndDamage(option),
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_AMS,
              option -> core.getRulesEquipment().getAMSReduction(option),
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_CHARGE_DAMAGE,
              option -> core.getRulesPhysical().getChargeDamage(charger, target, option, 2, 5)
                    + "/" + core.getRulesPhysical().getChargeDamageTakenBy(charger, 70.0, option, 5),
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RETRACTABLE_BLADES,
              option -> core.getRulesPhysical().retractableBladeArmCheck(option),
              // Both readers also require canUACsJam(), which Core answers false
              OptionsConstants.ADVANCED_COMBAT_UNJAM_UAC,
              option -> option && core.getRulesWeapons().canUACsJam(),
              OptionsConstants.INIT_FRONT_LOAD_INITIATIVE,
              option -> core.getRulesGame().getInitiativeOrder(turnsPerPlayer, 0, 2, option),
              OptionsConstants.ADVANCED_MINEFIELDS,
              option -> core.getRulesGame().allowMinefields(option),
              // Entity.getMASCorSuperchargerTarget only reads these under the Total Warfare rules manager
              OptionsConstants.ADVANCED_ALTERNATE_MASC,
              option -> option && isTotalWarfare(core),
              OptionsConstants.ADVANCED_ALTERNATE_MASC_ENHANCED,
              option -> option && isTotalWarfare(core));
    }

    private static boolean isTotalWarfare(Object rulesManager) {
        return rulesManager instanceof TWRulesManager;
    }

    @Test
    void everyIgnoredOptionHasACoreRuleCheck() {
        assertEquals(CoreRulesManager.IGNORED_GAME_OPTIONS, coreRulesReadingEachOption().keySet(),
              "Each option made read-only under Core needs a check here that Core ignores it");
    }

    @Test
    void coreRulesGiveTheSameResultWhetherAnIgnoredOptionIsOnOrOff() {
        for (Map.Entry<String, Function<Boolean, Object>> coreRule : coreRulesReadingEachOption().entrySet()) {
            assertEquals(coreRule.getValue().apply(false), coreRule.getValue().apply(true), coreRule.getKey());
        }
    }

    /** Issue #9730: Core Rules use Expanded Backward Movement for vehicles, so the player must be able to set it. */
    @Test
    void backwardMovementIsNotIgnoredUnderCore() {
        assertFalse(CoreRulesManager.IGNORED_GAME_OPTIONS
              .contains(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_WALK_BACKWARDS));
        Entity tank = new Tank();
        assertTrue(core.getRulesMovement().enableBackwardsElevationChange(true, tank));
        assertFalse(core.getRulesMovement().enableBackwardsElevationChange(false, tank));
    }
}

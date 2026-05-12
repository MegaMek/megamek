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
package megamek.client.ui.clientGUI.boardview;

import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;

/**
 * The three line-of-sight rule sets the engine implements. The Ruler tool reads the active rule from game options;
 * the diagram always draws the LOS line straight from eye level to eye level, but the per-hex blocker detection
 * follows the rule the engine is actually applying.
 *
 * <ul>
 *   <li>{@link #STANDARD} — BMM default. A hex's terrain intervenes if its top is at or above the
 *       attacker's LOS level (when attacker-adjacent), the target's LOS level (when target-adjacent), or the higher
 *       of the two LOS levels (when non-adjacent). The hex flagged as the blocker gets a red outline.</li>
 *   <li>{@link #DIAGRAMMED} — TO:AR p.77/78 (game option {@code TAC_OPS_LOS1}). The reference level is a linear
 *       interpolation between attacker and target LOS levels, evaluated at each hex's position along the path.</li>
 *   <li>{@link #DEAD_ZONE} — TacOps optional rule (game option {@code TAC_OPS_DEAD_ZONES}). Adds a geometric
 *       "shadow" check on top of Standard: if the tallest intervening hill projects a shadow that the lower unit
 *       sits inside, LOS is blocked. The lower unit's hex gets diagonal hatching with a "Dead Zone" label.</li>
 * </ul>
 */
enum LosRuleMode {
    STANDARD,
    DIAGRAMMED,
    DEAD_ZONE;

    /**
     * Selects the active rule based on which TacOps options the game has enabled. Diagrammed and Dead Zone are
     * mutually exclusive in the comparison table and treated the same way here.
     */
    static LosRuleMode fromGameOptions(Game game) {
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS1)) {
            return DIAGRAMMED;
        }
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_DEAD_ZONES)) {
            return DEAD_ZONE;
        }
        return STANDARD;
    }
}

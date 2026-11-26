/*
 * Copyright (C) 2019-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.common.weapons;

import java.util.Vector;

import jakarta.annotation.Nullable;
import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.weapons.handlers.WeaponHandler;

/**
 * Helper class that contains common functionality for flamer-type weapons.
 *
 * @author NickAragua
 */
public class FlamerHandlerHelper {
    /**
     * Handles flamer heat damage.
     */
    public static void doHeatDamage(Entity entityTarget, Vector<Report> vPhaseReport, WeaponType weaponType,
          int subjectId, HitData hit) {
        vPhaseReport.addElement(heatReport(entityTarget, weaponType, subjectId, hit));
    }

    /**
     * Applies the heat of a flamer that deals heat along with its damage, as the hit strikes and before its damage
     * removes armor (Core pp. 183, 201).
     *
     * @return The report of the heat, or null when the flamer deals no heat to the target
     */
    public static @Nullable Report heatWithDamage(Game game, Entity entityTarget, WeaponType weaponType,
          int subjectId, HitData hit) {
        boolean heatAndDamage = Game.rulesManager.getRulesWeapons()
              .flamerHeatAndDamage(game.getOptions().booleanOption(OptionsConstants.BASE_FLAMER_HEAT));
        return (heatAndDamage && entityTarget.tracksHeat()) ? heatReport(entityTarget, weaponType, subjectId, hit)
              : null;
    }

    private static Report heatReport(Entity entityTarget, WeaponType weaponType, int subjectId, HitData hit) {
        int heatDamage = weaponType.getDamage();

        // ER flamers don't do as much heat damage
        if (weaponType.hasFlag(WeaponType.F_ER_FLAMER)) {
            heatDamage = Math.max(1, heatDamage / 2);
        }

        return WeaponHandler.externalHeatReport(entityTarget, hit, heatDamage, subjectId, weaponType);
    }
}

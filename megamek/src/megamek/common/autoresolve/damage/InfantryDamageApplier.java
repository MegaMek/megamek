/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common.autoresolve.damage;

import megamek.common.BattleArmor;
import megamek.common.Infantry;

/**
 * @author Luana Coppio
 */
public record InfantryDamageApplier(Infantry entity) implements DamageApplier<Infantry> {

    @Override
    public boolean crewMustSurvive() {
        return false;
    }

    @Override
    public boolean entityMustSurvive() {
        return false;
    }

    @Override
    public HitDetails damageArmor(HitDetails hitDetails) {
        if (entity() instanceof BattleArmor te) {
            for (int i = 0; i < te.getTroopers(); i++) {
                var currentValueArmor = te.getArmor(BattleArmor.LOC_SQUAD);
                var newArmorValue = Math.max(currentValueArmor - 1, 0);
                if (te.getArmor(BattleArmor.LOC_TROOPER_1 + i) > 0) {
                    te.setArmor(newArmorValue, BattleArmor.LOC_TROOPER_1 + i);
                }
            }
        }
        if (entity().isCrippled()) {
            entity().setDestroyed(true);
        }
        return hitDetails;
    }

    @Override
    public HitDetails damageInternals(HitDetails hitDetails) {
        if (entity() instanceof BattleArmor te) {
            for (int i = 0; i < te.getTroopers(); i++) {
                var currentValue = te.getInternal(BattleArmor.LOC_SQUAD);
                var newValue = Math.max(currentValue - 1, 0);
                if (te.getInternal(BattleArmor.LOC_TROOPER_1 + i) > 0) {
                    te.setInternal(newValue, BattleArmor.LOC_TROOPER_1 + i);
                }
            }
        } else {
            var currentValue = entity().getInternal(Infantry.LOC_INFANTRY);
            var newValue = Math.max(currentValue - 1, 0);
            entity().setInternal(newValue, Infantry.LOC_INFANTRY);
        }

        if (entity().isCrippled()) {
            entity().setDestroyed(true);
        }
        return hitDetails;
    }
}

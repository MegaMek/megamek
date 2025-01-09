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
import megamek.common.Compute;
import megamek.common.Crew;
import megamek.common.Infantry;

/**
 * @author Luana Coppio
 */
public record InfantryDamageApplier(Infantry entity, EntityFinalState entityFinalState) implements DamageApplier<Infantry> {

    @Override
    public int devastateUnit() {
        int dmg = 0;
        if (entity() instanceof BattleArmor te) {
            for (int i = 0; i < te.getTroopers(); i++) {
                dmg += te.getArmor(BattleArmor.LOC_SQUAD);
                dmg += te.getInternal(BattleArmor.LOC_SQUAD);
                te.setInternal(0, BattleArmor.LOC_TROOPER_1 + i);

            }
        } else {
            dmg += entity().getInternal(Infantry.LOC_INFANTRY);
            entity().setInternal(0, Infantry.LOC_INFANTRY);
        }

        if (crewMayDie()) {
            var crew = entity().getCrew();
            for (int i = 0; i < crew.getSlotCount(); i++) {
                var hits = Compute.d6(2) >= 7 ? 5 : Crew.DEATH;
                entity().getCrew().setHits(hits, i);
            }
        }

        entity().setDestroyed(true);
        return dmg;
    }

    @Override
    public HitDetails damageArmor(HitDetails hitDetails) {
        if (entity() instanceof BattleArmor te) {
            var trooperId = te.getRandomTrooper();
            var currentValueArmor = te.getArmor(BattleArmor.LOC_SQUAD);
            var newArmorValue = Math.max(currentValueArmor - hitDetails.damageToApply(), 0);
            if (te.getArmor(trooperId) > 0) {
                te.setArmor(newArmorValue, trooperId);
            }
        }
        if (entity().isCrippled()) {
            entity().setDestroyed(true);
        }
        return hitDetails;
    }

    @Override
    public HitDetails damageInternals(HitDetails hitDetails) {
        if (noCrewDamage()) {
            return hitDetails;
        }
        if (entity() instanceof BattleArmor te) {
            var trooperId = te.getRandomTrooper();
            var currentValueArmor = te.getInternal(BattleArmor.LOC_SQUAD);
            var newArmorValue = Math.max(currentValueArmor - hitDetails.damageToApply(), 0);
            if (crewMustSurvive() || entityMustSurvive()) {
                newArmorValue = Math.max(newArmorValue, 1);
            }
            if (te.getInternal(trooperId) > 0) {
                te.setInternal(newArmorValue, trooperId);
            }
        } else {
            var currentValue = entity().getInternal(Infantry.LOC_INFANTRY);
            var newValue = Math.max(currentValue - 1, 0);
            if (crewMustSurvive() || entityMustSurvive()) {
                newValue = Math.max(newValue, 1);
            }
            entity().setInternal(newValue, Infantry.LOC_INFANTRY);
        }

        if (entity().isCrippled()) {
            entity().setDestroyed(true);
        }
        return hitDetails;
    }
}

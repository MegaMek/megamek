/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.autoresolve.damage;

import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.Crew;
import megamek.common.Infantry;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Luana Coppio
 */
public record InfantryDamageApplier(Infantry entity, EntityFinalState entityFinalState) implements DamageApplier<Infantry> {

    @Override
    public int getRandomHitLocation() {
        var entity = entity();
        if (entity instanceof BattleArmor) {
            return BattleArmor.LOC_SQUAD;
        }
        return Infantry.LOC_INFANTRY;
    }

    @Override
    public int devastateUnit() {
        int dmg = 0;
        if (entity() instanceof BattleArmor battleArmor) {
            for (int i = 0; i < battleArmor.getTroopers(); i++) {
                dmg += battleArmor.getArmor(BattleArmor.LOC_SQUAD);
                dmg += battleArmor.getInternal(BattleArmor.LOC_SQUAD);
                battleArmor.setInternal(0, BattleArmor.LOC_TROOPER_1 + i);
            }
        } else {
            dmg += entity().getInternal(Infantry.LOC_INFANTRY);
            entity().setInternal(0, Infantry.LOC_INFANTRY);
        }

        if (crewMayDie()) {
            var crew = entity().getCrew();
            for (int i = 0; i < crew.getSlotCount(); i++) {
                var hits = Compute.d6(2) >= 7 ? Compute.randomRealIntInclusive(4) + 1 : Crew.DEATH;
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
            if (trooperId == -1) {
                return hitDetails.killsCrew();
            }
            var currentValueArmor = te.getArmor(BattleArmor.LOC_SQUAD);
            var newArmorValue = Math.max(currentValueArmor - hitDetails.damageToApply(), 0);
            if (te.getArmor(trooperId) > 0) {
                te.setArmor(newArmorValue, trooperId);
            }
        } else {
            hitDetails = hitDetails.withInternalDamage(1);
        }
        if (entity().isCrippled()) {
            hitDetails = hitDetails.killsCrew();
        }
        return hitDetails;
    }

    @Override
    public void tryToDamageCrew(int hitCrew) {
        var crew = entity().getCrew();
        List<Integer> crewSlots = new ArrayList<>();

        for (int i = 0; i < crew.getSlotCount(); i++) {
            var hits = crew.getHits(i);
            if (hits > -1 && hits < 6) {
                crewSlots.add(i);
            }
        }
        if (crewSlots.isEmpty()) {
            DamageApplier.setEntityDestroyed(entity);
        } else {
            var idx = Compute.randomListElement(crewSlots);
            crew.setHits(hitCrew, idx);
        }
    }

    @Override
    public HitDetails damageInternals(HitDetails hitDetails) {
        if (entity() instanceof BattleArmor te) {
            var currentValueArmor = te.getInternal(BattleArmor.LOC_SQUAD);
            var newArmorValue = Math.max(currentValueArmor - hitDetails.damageToApply(), 0);
            if (crewMustSurvive() || entityMustSurvive()) {
                newArmorValue = Math.max(newArmorValue, 1);
            }
            te.setInternal(newArmorValue);
        } else {
            var currentValue = entity().getInternal(Infantry.LOC_INFANTRY);
            var newValue = Math.max(currentValue - 1, 0);
            entity().setInternal(newValue, Infantry.LOC_INFANTRY);
        }
        return hitDetails;
    }
}

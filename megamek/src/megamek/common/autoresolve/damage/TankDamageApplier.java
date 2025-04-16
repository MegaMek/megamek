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

import megamek.common.*;

/**
 * @author Luana Coppio
 */
public record TankDamageApplier(Tank entity, EntityFinalState entityFinalState) implements DamageApplier<Tank> {
    @Override
    public int devastateUnit() {
        int dmg = 0;
        dmg += entity.getInternal(0);
        entity.setInternal(0, 0);

        if (crewMayDie()) {
            var crew = entity().getCrew();
            for (int i = 0; i < crew.getSlotCount(); i++) {
                var hits = Compute.d6(2) >= 7 ? Compute.randomRealIntInclusive(4) + 1 : Crew.DEATH;
                entity().getCrew().setHits(hits, i);
            }
        }
        entity.applyDamage();
        return dmg;
    }

    @Override
    public HitDetails damageInternals(HitDetails hitDetails) {
        HitData hit = hitDetails.hit();
        var entity = entity();

        int currentInternalValue = entity.getInternal(hit);
        if (hitDetails.setArmorValueTo() > 0) {
            return hitDetails;
        }

        entity.setArmor(0, hit);
        int newInternalValue = Math.max(currentInternalValue + hitDetails.setArmorValueTo(), entityMustSurvive() ? 1 : 0);

        logger.trace("[{}] Damage: {} - Internal at: {}", entity.getDisplayName(), hitDetails.damageToApply(), newInternalValue);
        entity.setInternal(newInternalValue, hit);
        if (hitDetails.hitInternal() == 3) {
            hitDetails = hitDetails.killsCrew();
        } else if (newInternalValue > 0) {
            hitDetails = hitDetails.withIncreasedCrewDamage();
            hitDetails = applyDamageToEquipments(hitDetails);
        } else {
            hitDetails = destroyLocation(hitDetails);
        }



        return hitDetails;
    }
}

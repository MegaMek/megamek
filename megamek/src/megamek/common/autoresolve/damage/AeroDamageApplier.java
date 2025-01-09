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

import megamek.common.Aero;
import megamek.common.Compute;
import megamek.common.HitData;
import megamek.common.IEntityRemovalConditions;

import java.util.ArrayList;
import java.util.List;

import static megamek.common.Compute.randomInt;

/**
 * Applies damage to an Aero entity.
 * @param entity Aero entity
 * @param entityFinalState The final state of the entity
 * @author Luana Coppio
 */
public record AeroDamageApplier(Aero entity, EntityFinalState entityFinalState) implements DamageApplier<Aero> {

    @Override
    public int devastateUnit() {
        int totalDamageToApply = entity().getSI();
        int res = Integer.MAX_VALUE;
        int loc = -1;
        for (int i = 0; i < entity().locations(); i++) {
            if (entity().getArmor(i) < res && entity().getArmor(i) != entity().getOArmor(i)) {
                res = entity().getArmor(i);
                loc = i;
            }
        }
        if (loc > -1) {
            entity().setArmor(0, loc);
            totalDamageToApply += res;
        } else {
            var newLoc = Compute.randomIntInclusive(entity().locations());
            totalDamageToApply += entity().getArmor(newLoc);
            entity().setArmor(0, newLoc);
        }

        entity().setSI(0);
        entity().setDestroyed(true);
        return totalDamageToApply;
    }

    @Override
    public int getRandomHitLocation() {
        var entity = entity();
        List<Integer> validLocations = new ArrayList<>();
        for (int i = 0; i < entity.locations(); i++) {
            if (entity.getOArmor(i) <= 0) {
                continue;
            }
            if (!entity.isLocationBlownOff(i) && entity.getSI() > 0) {
                validLocations.add(i);
            }
        }

        return validLocations.isEmpty() ? -1 : validLocations.get(randomInt(validLocations.size()));
    }

    @Override
    public void destroyLocationAfterEjection() {
        entity().setDestroyed(true);
        entity().setRemovalCondition(IEntityRemovalConditions.REMOVE_DEVASTATED);
    }

    @Override
    public HitDetails damageInternals(HitDetails hitDetails) {
        HitData hit = hitDetails.hit();
        var entity = entity();
        int currentInternalValue = entity.getSI();
        int newInternalValue = Math.max(currentInternalValue + hitDetails.setArmorValueTo(), 0);
        entity.setArmor(0, hit);
        entity.setSI(newInternalValue);
        applyDamageToEquipments(hit);
        if (newInternalValue == 0) {
            hitDetails = destroyLocation(hitDetails);
        }
        return hitDetails;
    }
}

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
import megamek.common.HitData;
import megamek.common.IEntityRemovalConditions;

import java.util.ArrayList;
import java.util.List;

import static megamek.common.Compute.randomInt;

/**
 * Applies damage to an Aero entity.
 * @param entity Aero entity
 * @param crewMustSurvive Whether the crew must survive
 * @param entityMustSurvive Whether the entity must survive
 * @author Luana Coppio
 */
public record AeroDamageApplier(Aero entity, boolean crewMustSurvive, boolean entityMustSurvive, boolean noCrewDamage) implements DamageApplier<Aero> {

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

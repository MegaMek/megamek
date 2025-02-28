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

import megamek.common.Compute;
import megamek.common.Crew;
import megamek.common.Entity;

/**
 * @author Luana Coppio
 */
public record SimpleDamageApplier(Entity entity, EntityFinalState entityFinalState) implements DamageApplier<Entity> {
    @Override
    public int devastateUnit() {
        var loc = Compute.randomInt(entity().locations());
        var totalDamage = entity().getArmor(loc) + entity().getInternal(loc);
        entity().setArmor(0, loc);
        entity().setInternal(0, loc);
        if (crewMayDie()) {
            var crew = entity().getCrew();
            for (int i = 0; i < crew.getSlotCount(); i++) {
                var hits = Compute.d6(2) >= 7 ? 5 : Crew.DEATH;
                entity().getCrew().setHits(hits, i);
            }
        }

        return totalDamage;
    }
}

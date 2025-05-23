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

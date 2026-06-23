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
package megamek.server.totalWarfare;

import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Applies the delayed Corrosive Ammo (acid) damage queued by Fluid Gun / Sprayer attacks during the
 * Weapon Attack Phase. Each affected unit takes its queued 1D6/2 (round up) of damage to structure in
 * 1-point clusters during the End Phase of the same turn (TO:AUE p.173).
 *
 * @author The MegaMek Team
 */
class CorrosiveDamageHandler extends AbstractTWRuleHandler {
    private static final MMLogger LOGGER = MMLogger.create(CorrosiveDamageHandler.class);

    CorrosiveDamageHandler(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * Applies and clears every unit's queued End-Phase corrosive damage, then promotes any following-turn
     * corrosive damage (from a Corrosive Ammo crit explosion) so it lands at the next End Phase.
     */
    void resolveCorrosiveDamage() {
        for (Entity entity : getGame().getEntitiesVector()) {
            int pendingDamage = entity.getPendingCorrosiveDamage();
            if (pendingDamage > 0) {
                if (entity.isDestroyed() || entity.isDoomed()) {
                    // The unit is already gone; just drop the queued damage.
                    entity.clearPendingCorrosiveDamage();
                } else {
                    LOGGER.debug("[Corrosive] {}: applying {} queued End-Phase corrosive damage point(s)",
                          entity.getShortName(), pendingDamage);

                    Report header = new Report(3392);
                    header.subject = entity.getId();
                    header.addDesc(entity);
                    header.add(pendingDamage);
                    addReport(header);

                    // Resolved in 1-point clusters, each rolling its own hit location (TO:AUE p.173).
                    for (int point = 0; point < pendingDamage; point++) {
                        HitData hit = entity.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
                        addReport(gameManager.damageEntity(entity, hit, 1));
                    }
                    entity.clearPendingCorrosiveDamage();
                }
            }

            // A Corrosive Ammo crit explosion queues damage for the *following* turn's End Phase; promote it
            // now (after this turn's pending damage is applied) so it lands one turn later (TO:AUE p.173).
            entity.promoteNextTurnCorrosiveDamage();
        }
    }
}

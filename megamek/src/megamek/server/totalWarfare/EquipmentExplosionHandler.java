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

import java.util.Vector;

import megamek.common.CriticalSlot;
import megamek.common.Report;
import megamek.common.equipment.Mounted;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Sets off one piece of a unit's equipment at a gamemaster's request, the way a critical hit sets it off in play:
 * an ammo bin, a hyper-velocity autocannon, a RISC hyper laser, a charged capacitor, a launcher with hot-loaded
 * ammo. The equipment and its critical slots are marked hit first, as the critical-hit resolution does, and the
 * explosion itself is the same server resolution a crit uses, so the damage, CASE, the pilot hits, inferno heat,
 * the hot-load chain reaction and any destruction of the unit all come out as they would in play. Only the
 * trigger differs: the gamemaster says it explodes, so there is no to-hit roll and no Edge reroll.
 */
public class EquipmentExplosionHandler extends AbstractTWRuleHandler {

    private static final MMLogger LOGGER = MMLogger.create(EquipmentExplosionHandler.class);

    /** What became of the request: the one success, and the reasons it can be refused. */
    public enum Outcome {
        /** The equipment exploded and its damage was resolved. */
        EXPLODED,
        /** The equipment was already destroyed, so there was nothing left to explode. */
        ALREADY_DESTROYED,
        /** Nothing would explode: the equipment is not explosive in its current state, or would do no damage. */
        NOT_EXPLOSIVE
    }

    EquipmentExplosionHandler(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * Explodes the given equipment on the given unit, if a critical hit on it would explode right now.
     *
     * @param entity  the unit carrying the equipment
     * @param mounted the equipment to set off
     *
     * @return what happened, for the gamemaster to be told
     */
    public Outcome explode(Entity entity, Mounted<?> mounted) {
        if (mounted.isDestroyed()) {
            LOGGER.info("[Explode] refused: {} on {} is already destroyed", mounted.getName(),
                  entity.getDisplayName());
            return Outcome.ALREADY_DESTROYED;
        }
        if (!mounted.wouldExplodeWhenHit()) {
            LOGGER.info("[Explode] refused: {} on {} would not explode (explosive {}, damage {})",
                  mounted.getName(), entity.getDisplayName(), mounted.getType().isExplosive(mounted),
                  mounted.getExplosionDamage());
            return Outcome.NOT_EXPLOSIVE;
        }

        // a critical hit marks the equipment and its slots hit before the explosion is resolved; so does this
        mounted.setHit(true);
        markCriticalSlotsHit(entity, mounted, mounted.getLocation());
        if (mounted.isSplit()) {
            markCriticalSlotsHit(entity, mounted, mounted.getSecondLocation());
        }

        // a hot-loaded launcher is not explosive by type, so the crit resolution overrides the check for it
        Vector<Report> reports = gameManager.explodeEquipment(entity, mounted.getLocation(), mounted,
              mounted.isHotLoaded());
        addReport(reports);
        gameManager.entityUpdate(entity.getId());
        LOGGER.info("[Explode] {} on {} exploded at the gamemaster's request; unit doomed {}, crew hits {}, "
                    + "crew ejected {}",
              mounted.getName(), entity.getDisplayName(), entity.isDoomed(),
              (entity.getCrew() == null) ? "none" : entity.getCrew().getHits(),
              (entity.getCrew() != null) && entity.getCrew().isEjected());
        // the blast may have destroyed the unit or ejected its crew mid-turn; the turn order has to follow
        new GamemasterTurnUpkeep(gameManager).settleTurnsAfter(entity);
        return Outcome.EXPLODED;
    }

    /** Marks every critical slot of the location that holds the equipment as hit. */
    private void markCriticalSlotsHit(Entity entity, Mounted<?> mounted, int location) {
        for (int slot = 0; slot < entity.getNumberOfCriticalSlots(location); slot++) {
            CriticalSlot criticalSlot = entity.getCritical(location, slot);
            if ((criticalSlot != null) && (criticalSlot.getType() == CriticalSlot.TYPE_EQUIPMENT)
                  && mounted.equals(criticalSlot.getMount())) {
                criticalSlot.setHit(true);
            }
        }
    }
}

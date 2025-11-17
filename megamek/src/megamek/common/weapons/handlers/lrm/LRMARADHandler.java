/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.weapons.handlers.lrm;

import java.io.Serial;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.compute.ComputeECM;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;
import megamek.common.weapons.handlers.ARADEquipmentDetector;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Weapon handler for ARAD (Anti-Radiation) LRM missiles.
 *
 * ARAD missiles receive cluster bonuses against targets with active electronics,
 * and penalties against targets without electronics. ECM can block the bonus
 * unless the target is Narc-tagged (Narc overrides ECM).
 *
 * Cluster Modifiers:
 * - +1 against targets with qualifying electronics (unless blocked by ECM)
 * - 0 if target has electronics but ECM blocks (and no Narc override)
 * - -2 against targets without electronics (minimum 2 hits enforced by engine)
 *
 * Rules Reference: Tactical Operations: Advanced Units & Equipment, p.180
 *
 * @author MegaMek Team
 * @since 2025-01-16
 */
public class LRMARADHandler extends LRMHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    @Serial
    private static final long serialVersionUID = -8675309867530986753L;

    public LRMARADHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m)
            throws EntityLoadingException {
        super(t, w, g, m);
    }

    /**
     * Calculate ARAD cluster modifier based on target electronics and ECM status.
     *
     * Logic:
     * 1. Check if target is an Entity (only Entities have electronics)
     * 2. Check if target has qualifying electronics (via ARADEquipmentDetector)
     * 3. If YES electronics:
     *    - Check if Narc-tagged → +1 (Narc overrides ECM)
     *    - Check if ECM-affected → 0 (ECM blocks bonus)
     *    - Otherwise → +1 (standard bonus)
     * 4. If NO electronics → -2 (penalty, minimum 2 enforced automatically)
     *
     * @return Cluster modifier for ARAD missiles
     */
    @Override
    public int getSalvoBonus() {
        LOGGER.debug("ARAD getSalvoBonus() called - attacker: {}, target: {}",
                attackingEntity.getDisplayName(), target.getDisplayName());

        // Only Entities can have electronics
        if (target.getTargetType() != Targetable.TYPE_ENTITY) {
            LOGGER.debug("ARAD: Target is not an entity, returning -2");
            return -2;  // Non-entity targets have no electronics
        }

        Entity entityTarget = (Entity) target;
        int friendlyTeam = attackingEntity.getOwner().getTeam();

        // Check if target has qualifying electronics
        boolean hasElectronics = ARADEquipmentDetector.targetHasQualifyingElectronics(
                entityTarget, friendlyTeam);
        LOGGER.debug("ARAD: Target has electronics: {}", hasElectronics);

        if (hasElectronics) {
            // Target has electronics - check for ECM interference

            // Narc-tagged targets ALWAYS get bonus (Narc overrides ECM)
            boolean isNarcTagged = ARADEquipmentDetector.isNarcTagged(entityTarget, friendlyTeam);
            LOGGER.debug("ARAD: Target is Narc-tagged: {}", isNarcTagged);
            if (isNarcTagged) {
                LOGGER.debug("ARAD: Narc override - returning +1");
                return +1;  // Narc overrides ECM
            }

            // Check if flight path is ECM-affected (matches Artemis IV pattern)
            // ECM affects the missile flight path from attacker to target
            boolean isECMAffected = ComputeECM.isAffectedByECM(
                    attackingEntity,
                    attackingEntity.getPosition(),
                    target.getPosition());
            LOGGER.debug("ARAD: Flight path ECM-affected: {}", isECMAffected);

            if (isECMAffected) {
                LOGGER.debug("ARAD: ECM blocking - returning 0");
                return 0;  // ECM blocks bonus (but no penalty)
            }

            // Target has electronics, no ECM interference
            LOGGER.debug("ARAD: Electronics, no ECM - returning +1");
            return +1;  // Standard ARAD bonus
        } else {
            // Target has NO electronics
            LOGGER.debug("ARAD: No electronics - returning -2");
            return -2;  // ARAD penalty (minimum 2 hits enforced by Compute.missilesHit)
        }
    }
}

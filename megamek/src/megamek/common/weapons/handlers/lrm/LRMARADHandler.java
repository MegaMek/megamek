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
import java.util.Vector;

import megamek.common.Report;
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
 * ARAD missiles receive cluster bonuses against targets with active electronics,
 * and penalties against targets without electronics. ECM can block the bonus
 * unless the target is Narc-tagged (Narc overrides ECM).
 *
 * Cluster Modifiers:
 * - +1 against targets with qualifying electronics (unless blocked by ECM)
 * - 0 if target has electronics but ECM blocks (and no Narc override)
 * - -2 against targets without electronics (minimum 2 hits enforced by engine)
 *
 * Rules Reference: Tactical Operations: Advanced Units &amp; Equipment, p.180
 * Quote: "ARAD missiles ignore hostile ECM effects when targeting a unit tagged
 * by a friendly Narc pod. However, the ARAD missile does not receive any further
 * to-hit bonus from the pod."
 *
 * Forum Ruling (Narc/ECM interaction):
 * https://battletech.com/forums/index.php?topic=26824.msg609067#msg609067
 *
 * @author MegaMek Team
 * @since 2025-01-16
 */
public class LRMARADHandler extends LRMHandler {
    @Serial
    private static final long serialVersionUID = -8675309867530986753L;

    public LRMARADHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m)
            throws EntityLoadingException {
        super(t, w, g, m);
    }

    /**
     * Calculate ARAD cluster modifier based on target electronics and ECM status.
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
        // Only Entities can have electronics
        if (target.getTargetType() != Targetable.TYPE_ENTITY) {
            return -2;  // Non-entity targets have no electronics
        }

        Entity entityTarget = (Entity) target;
        int friendlyTeam = attackingEntity.getOwner().getTeam();

        // Check if target has qualifying electronics
        boolean hasElectronics = ARADEquipmentDetector.targetHasQualifyingElectronics(
                entityTarget, friendlyTeam);

        if (hasElectronics) {
            // Target has electronics - check for ECM interference

            // Narc-tagged targets ALWAYS get bonus (Narc overrides ECM)
            // TO:AUE p.180: "ignore hostile ECM effects when targeting a unit tagged by a friendly Narc pod"
            // However, ARAD does NOT receive additional Narc-specific bonuses (no stacking)
            // Forum ruling: https://battletech.com/forums/index.php?topic=26824.msg609067#msg609067
            if (ARADEquipmentDetector.isNarcTagged(entityTarget, friendlyTeam)) {
                return +1;  // Narc overrides ECM, but no additional Narc bonus
            }

            // Check if flight path is ECM-affected (matches Artemis IV pattern)
            // ECM affects the missile flight path from attacker to target
            boolean isECMAffected = ComputeECM.isAffectedByECM(
                    attackingEntity,
                    attackingEntity.getPosition(),
                    target.getPosition());

            if (isECMAffected) {
                return 0;  // ECM blocks bonus for non-Narc targets (but no penalty applied)
            }

            // Target has electronics, no ECM interference
            return +1;  // Standard ARAD bonus
        } else {
            // Target has NO electronics
            return -2;  // ARAD penalty (minimum 2 hits enforced by Compute.missilesHit)
        }
    }

    /**
     * Override calcHits() to add ARAD-specific report messages for cluster modifiers.
     * This method adds visible feedback to players about ARAD cluster modifier state:
     * - Report 3363: ECM blocked bonus
     * - Report 3364: Narc override (bonus despite ECM)
     * - Report 3368: No electronics penalty
     * - Report 3369: Normal bonus (electronics detected)
     *
     * @param vPhaseReport Vector to collect report messages
     * @return Number of missiles that hit
     */
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        // Call parent to calculate hits using our getSalvoBonus()
        int hits = super.calcHits(vPhaseReport);

        // Add ARAD-specific reporting based on cluster modifier state
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            Entity entityTarget = (Entity) target;
            int friendlyTeam = attackingEntity.getOwner().getTeam();

            boolean hasElectronics = ARADEquipmentDetector.targetHasQualifyingElectronics(
                    entityTarget, friendlyTeam);

            if (hasElectronics) {
                boolean isNarcTagged = ARADEquipmentDetector.isNarcTagged(entityTarget, friendlyTeam);
                boolean isECMAffected = ComputeECM.isAffectedByECM(
                        attackingEntity,
                        attackingEntity.getPosition(),
                        target.getPosition());

                if (isECMAffected && !isNarcTagged) {
                    // ECM blocked bonus (Report 3363)
                    Report r = new Report(3363);
                    r.subject = subjectId;
                    r.newlines = 0;
                    vPhaseReport.addElement(r);
                } else if (isNarcTagged && isECMAffected) {
                    // Narc override (Report 3364) - bonus applied despite ECM
                    Report r = new Report(3364);
                    r.subject = subjectId;
                    r.newlines = 0;
                    vPhaseReport.addElement(r);
                } else {
                    // Normal bonus (Report 3369) - electronics detected, no ECM interference
                    Report r = new Report(3369);
                    r.subject = subjectId;
                    r.newlines = 0;
                    vPhaseReport.addElement(r);
                }
            } else {
                // No electronics - penalty applied (Report 3368)
                Report r = new Report(3368);
                r.subject = subjectId;
                r.newlines = 0;
                vPhaseReport.addElement(r);
            }
        } else {
            // Non-entity target (buildings, hexes) - penalty (Report 3368)
            Report r = new Report(3368);
            r.subject = subjectId;
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }

        return hits;
    }
}

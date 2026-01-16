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
package megamek.common.weapons.handlers.lrm;

import java.io.Serial;
import java.util.Vector;

import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;
import megamek.common.weapons.handlers.ARADEquipmentDetector;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Weapon handler for ARAD (Anti-Radiation) LRM missiles. ARAD missiles receive cluster bonuses against targets with
 * active electronics, and penalties against targets without electronics.  ARAD missiles override the ECM malus
 * against NARC, allowing ARAD missiles to gain their bonuses against NARC-tagged targets even when affected by ECM.
 * <p>
 * Cluster Modifiers:
 * <ul>
 *   <li>+1 against targets with qualifying electronics</li>
 *   <li>+1 against Narc-tagged targets (even if under ECM)</li>
 *   <li>-2 against targets without electronics (minimum 2 hits enforced by engine)</li>
 * </ul>
 * <p>
 * Rules Reference: Tactical Operations: Advanced Units &amp; Equipment, p.180 Quote: "ARAD missiles ignore hostile ECM
 * effects when targeting a unit tagged by a friendly Narc pod. However, the ARAD missile does not receive any further
 * to-hit bonus from the pod."
 * <p>
 * Forum Ruling (Narc/ECM interaction): https://battletech.com/forums/index.php?topic=26824.msg609067#msg609067
 *
 * @author Hammer - Built with Claude Code
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
     * <p>
     * Logic:
     * <ol>
     *   <li>Check if target is an Entity (only Entities have electronics)</li>
     *   <li>Check if target has qualifying electronics (via ARADEquipmentDetector)</li>
     *   <li>Check if Narc-tagged &rarr; +1 (ARAD overrides ECM vs NARC)</li>
     *   <li>If YES electronics: +1 (standard bonus)</li>
     *   <li>If NO electronics or NARC &rarr; -2 (penalty, minimum 2 enforced automatically)</li>
     * </ol>
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
            // Target has electronics
            return +1;  // Standard ARAD bonus

        } else {
            // Narc-tagged targets ALWAYS get bonus (Narc overrides ECM)
            // TO:AUE p.180: "ignore hostile ECM effects when targeting a unit tagged by a friendly Narc pod"
            // However, ARAD does NOT receive additional Narc-specific bonuses (no stacking)
            // Forum ruling: https://battletech.com/forums/index.php?topic=26824.msg609067#msg609067
            if (ARADEquipmentDetector.isNarcTagged(entityTarget, friendlyTeam)) {
                return +1;  // Narc overrides ECM, but no additional Narc bonus
            }

            // Target has NO electronics
            return -2;  // ARAD penalty (minimum 2 hits enforced by Compute.missilesHit)
        }
    }

    /**
     * Override calcHits() to add ARAD-specific report messages for cluster modifiers.
     * <p>
     * This method adds visible feedback to players about ARAD cluster modifier state:
     * <ul>
     *   <li>Report 3364: Narc override (bonus despite ECM)</li>
     *   <li>Report 3368: No electronics penalty</li>
     *   <li>Report 3369: Normal bonus (electronics detected)</li>
     * </ul>
     *
     * @param vPhaseReport Vector to collect report messages
     *
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

            boolean isNarcTagged = ARADEquipmentDetector.isNarcTagged(entityTarget, friendlyTeam);

            if (isNarcTagged) {
                // Narc provides bonus (Report 3364)
                Report r = new Report(3364);
                r.subject = subjectId;
                r.newlines = 0;
                vPhaseReport.addElement(r);
            } else if (hasElectronics) {
                // Normal bonus (Report 3369) - electronics detected, no ECM interference
                Report r = new Report(3369);
                r.subject = subjectId;
                r.newlines = 0;
                vPhaseReport.addElement(r);
            } else {
                // No electronics or Narc - penalty applied (Report 3368)
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

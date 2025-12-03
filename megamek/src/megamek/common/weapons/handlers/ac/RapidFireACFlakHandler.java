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

package megamek.common.weapons.handlers.ac;

import java.io.Serial;
import java.util.Vector;

import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.compute.Compute;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.units.Infantry;
import megamek.common.weapons.handlers.RapidFireACWeaponHandler;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Handles rapid-fire autocannon attacks with flak ammunition. Combines the rapid-fire mechanics (jam checks, cluster
 * table) with the flak-specific damage handling (5 fragments, cluster table rolls).
 *
 * @see RapidFireACWeaponHandler
 * @see ACFlakHandler
 */
public class RapidFireACFlakHandler extends RapidFireACWeaponHandler {
    @Serial
    private static final long serialVersionUID = -7814754695629392L;

    public RapidFireACFlakHandler(ToHitData toHitData, WeaponAttackAction weaponAttackAction, Game game,
          TWGameManager twGameManager) throws EntityLoadingException {
        super(toHitData, weaponAttackAction, game, twGameManager);
        sSalvoType = " fragment(s) ";
    }

    /**
     * Each fragment does 1 damage (like LBX pellets). "5-point clusters" means 5 fragments grouped to one location = 5
     * damage.
     */
    @Override
    protected int calcDamagePerHit() {
        if (target.isConventionalInfantry()) {
            double toReturn = Compute.directBlowInfantryDamage(
                  weaponType.getDamage(), bDirect ? toHit.getMoS() / 3 : 0,
                  WeaponType.WEAPON_CLUSTER_BALLISTIC,
                  ((Infantry) target).isMechanized(),
                  toHit.getThruBldg() != null, attackingEntity.getId(), calcDmgPerHitReport);
            toReturn = applyGlancingBlowModifier(toReturn, true);
            return (int) toReturn;
        }
        return 1;
    }

    /**
     * Calculate number of fragments that hit in rapid-fire mode. Uses two-stage approach per rapid-fire rules: 1. Roll
     * cluster for howManyShots (2) to determine shots that connect 2. For each connecting shot, roll cluster for its
     * fragments (weapon damage)
     * <p>
     * For AC/10 rapid-fire flak: - Stage 1: Roll cluster for 2 -> 1 or 2 shots connect - Stage 2: Each shot = 10
     * fragments, roll cluster for each - calculateNumCluster() = 5 groups fragments into 5-point clusters for location
     * rolls
     */
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        // Conventional infantry gets hit in one lump
        if (target.isConventionalInfantry()) {
            return 1;
        }

        int nHitsModifier = getClusterModifiers(true);
        PlanetaryConditions conditions = game.getPlanetaryConditions();

        // STAGE 1: Rapid-fire cluster roll - how many of the 2 shots connect?
        int shotsHit;
        if (allShotsHit()) {
            shotsHit = howManyShots;
            if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RANGE)
                  && (nRange > weaponType.getRanges(weapon)[RangeType.RANGE_LONG])) {
                shotsHit = (int) Math.ceil(shotsHit * .75);
            }
            if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS_RANGE)
                  && (nRange > weaponType.getRanges(weapon)[RangeType.RANGE_EXTREME])) {
                shotsHit = (int) Math.ceil(shotsHit * .5);
            }
        } else {
            shotsHit = Compute.missilesHit(howManyShots, nHitsModifier, conditions.getEMI().isEMI());
        }

        // Report rapid-fire shots hit
        Report report = new Report(3325);
        report.subject = subjectId;
        report.add(shotsHit);
        report.add(" shot(s) ");
        report.add(toHit.getTableDesc());
        report.newlines = 0;
        vPhaseReport.addElement(report);
        if (nHitsModifier != 0) {
            if (nHitsModifier > 0) {
                report = new Report(3340);
            } else {
                report = new Report(3341);
            }
            report.subject = subjectId;
            report.add(nHitsModifier);
            report.newlines = 0;
            vPhaseReport.addElement(report);
        }
        // Close shots report with newline
        report = new Report(3345);
        report.subject = subjectId;
        vPhaseReport.addElement(report);

        if (shotsHit == 0) {
            bSalvo = true;
            return 0;
        }

        // STAGE 2: Each connecting shot applies flak rules independently
        // Each shot produces fragments equal to weapon damage (AC/10 = 10 fragments)
        int fragmentsPerShot = weaponType.getDamage();  // AC/10 = 10 fragments per shot
        int totalFragmentsHit = 0;
        StringBuilder perShotBreakdown = new StringBuilder();

        for (int shot = 1; shot <= shotsHit; shot++) {
            int fragmentsHit;
            if (allShotsHit()) {
                fragmentsHit = fragmentsPerShot;
            } else {
                fragmentsHit = Compute.missilesHit(fragmentsPerShot, nHitsModifier, conditions.getEMI().isEMI());
            }
            totalFragmentsHit += fragmentsHit;

            // Build per-shot breakdown string
            if (shot > 1) {
                perShotBreakdown.append(", ");
            }
            perShotBreakdown.append("Shot ").append(shot).append(": ").append(fragmentsHit);
        }

        // Report fragments with per-shot breakdown (indented)
        report = new Report(3325);
        report.subject = subjectId;
        report.add(totalFragmentsHit);
        report.add(sSalvoType);
        report.add("(" + perShotBreakdown + ")");
        report.indent();
        report.newlines = 0;
        vPhaseReport.addElement(report);
        report = new Report(3345);
        report.subject = subjectId;
        vPhaseReport.addElement(report);

        bSalvo = true;
        return totalFragmentsHit;
    }

    @Override
    protected int calculateNumCluster() {
        // Group 5 fragments per location = 5 damage per location ("5-point clusters")
        return 5;
    }

    @Override
    protected boolean usesClusterTable() {
        return ammo.getType().getMunitionType().contains(AmmoType.Munitions.M_FLAK);
    }
}

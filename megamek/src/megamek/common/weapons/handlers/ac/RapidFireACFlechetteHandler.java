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
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.IBuilding;
import megamek.common.weapons.DamageType;
import megamek.common.weapons.handlers.RapidFireACWeaponHandler;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Handles rapid-fire autocannon attacks with flechette ammunition. Combines the rapid-fire mechanics (jam checks,
 * cluster table) with the flechette-specific damage handling (double damage to woods/jungle).
 *
 * @see RapidFireACWeaponHandler
 * @see ACFlechetteHandler
 */
public class RapidFireACFlechetteHandler extends RapidFireACWeaponHandler {
    @Serial
    private static final long serialVersionUID = 7965585014230085L;

    private static final MMLogger LOGGER = MMLogger.create(RapidFireACFlechetteHandler.class);

    public RapidFireACFlechetteHandler(ToHitData toHitData, WeaponAttackAction weaponAttackAction, Game game,
          TWGameManager twGameManager) throws EntityLoadingException {
        super(toHitData, weaponAttackAction, game, twGameManager);
        damageType = DamageType.FLECHETTE;
    }

    /**
     * Calculate number of shots that hit in rapid-fire mode. Flechette treats infantry and non-infantry the same - roll
     * cluster for both. Per flechette rules, damage is applied "as though from an infantry unit" so each shot does
     * weapon damage (no reduction table).
     */
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        LOGGER.debug("calcHits() called - target: {}, isInfantry: {}, howManyShots: {}",
              target.getDisplayName(), target.isConventionalInfantry(), howManyShots);

        // Roll cluster table for all targets (infantry and non-infantry alike)
        int nHitsModifier = getClusterModifiers(true);
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
            shotsHit = Compute.missilesHit(howManyShots, nHitsModifier,
                  game.getPlanetaryConditions().getEMI().isEMI());
        }

        LOGGER.debug("Shots hit: {} (modifier: {})", shotsHit, nHitsModifier);

        // Report shots hit
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
        report = new Report(3345);
        report.subject = subjectId;
        vPhaseReport.addElement(report);

        if (shotsHit == 0) {
            bSalvo = true;
            return 0;
        }

        bSalvo = true;
        return shotsHit;
    }

    /**
     * Calculate damage per hit. Flechette applies standard weapon damage to infantry (no reduction table). Half damage
     * to non-infantry is handled by DamageType.FLECHETTE in TWDamageManager.
     */
    @Override
    protected int calcDamagePerHit() {
        double toReturn = weaponType.getDamage();
        LOGGER.debug("calcDamagePerHit() - base damage: {}, isInfantry: {}",
              toReturn, target.isConventionalInfantry());

        // Apply glancing blow modifier if applicable
        toReturn = applyGlancingBlowModifier(toReturn, false);

        // Apply TacOps range modifiers
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RANGE)
              && (nRange > weaponType.getRanges(weapon)[RangeType.RANGE_LONG])) {
            toReturn = (int) Math.floor(toReturn * .75);
        }
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS_RANGE)
              && (nRange > weaponType.getRanges(weapon)[RangeType.RANGE_EXTREME])) {
            toReturn = (int) Math.floor(toReturn * .5);
        }

        LOGGER.debug("calcDamagePerHit() - final damage: {}", (int) toReturn);
        return (int) toReturn;
    }

    @Override
    protected void handleClearDamage(Vector<Report> vPhaseReport, IBuilding bldg, int nDamage) {
        if (!bSalvo) {
            // hits!
            Report r = new Report(2270);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }
        // Flechette weapons do double damage to woods
        nDamage *= 2;

        // report that damage was "applied" to terrain
        Report r = new Report(3385);
        r.indent(2);
        r.subject = subjectId;
        r.add(nDamage);
        r.newlines = 0;
        vPhaseReport.addElement(r);

        // Any clear attempt can result in accidental ignition, even
        // weapons that can't normally start fires. that's weird.
        // Buildings can't be accidentally ignited.
        if ((bldg != null)
              && gameManager.tryIgniteHex(target.getPosition(), target.getBoardId(), subjectId, false, false,
              new TargetRoll(weaponType.getFireTN(), weaponType.getName()), 5, vPhaseReport)) {
            return;
        }

        Vector<Report> clearReports = gameManager.tryClearHex(target.getPosition(), target.getBoardId(), nDamage,
              subjectId);
        if (!clearReports.isEmpty()) {
            vPhaseReport.lastElement().newlines = 0;
        }
        vPhaseReport.addAll(clearReports);
    }
}

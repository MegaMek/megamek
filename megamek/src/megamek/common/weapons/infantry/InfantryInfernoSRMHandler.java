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

package megamek.common.weapons.infantry;

import java.io.Serial;
import java.util.Vector;

import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Resolves a conventional infantry SRM platoon firing true Inferno munitions.
 *
 * <p>TW p. 143: "An SRM infantry platoon that hits its target does so with a number of inferno missiles equal to
 * its Damage Value after rolling on the Cluster Hits Table, divided by 2 (round fractions down)." The missiles are
 * delivered instead of the platoon's ordinary damage, not in addition to it, which is why the launcher offers
 * Inferno and Damage as alternative firing modes.</p>
 *
 * <p>This is what separates a real Inferno SRM from the incendiary support weapons renamed by the TechManual
 * pp. 350-352 errata. An incendiary weapon in Heat mode only converts its damage to heat, and so does nothing to a
 * target that does not track heat. Inferno missiles carry their own effects against every unit type: heat on
 * Meks and fighters, automatic critical hit rolls on vehicles, and three troopers killed per missile against
 * conventional infantry (TW pp. 142-143).</p>
 */
public class InfantryInfernoSRMHandler extends InfantryWeaponHandler {

    @Serial
    private static final long serialVersionUID = -1541176315633613267L;

    /** The platoon's Damage Value for this attack, kept for reporting. */
    private int damageValue;

    /** Missiles delivered by this attack, worked out once the platoon's Damage Value is known. */
    private int infernoMissiles;

    public InfantryInfernoSRMHandler(ToHitData toHitData, WeaponAttackAction weaponAttackAction, Game game,
          TWGameManager twGameManager) throws EntityLoadingException {
        super(toHitData, weaponAttackAction, game, twGameManager);
    }

    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        int hits = super.calcHits(vPhaseReport);

        // super.calcHits() reports the platoon's Damage Value and returns it, except against conventional
        // infantry, where it returns a single hit and puts the Damage Value in nDamPerHit instead.
        damageValue = target.isConventionalInfantry() ? nDamPerHit : hits;
        infernoMissiles = damageValue / 2;

        Report report = new Report(3331);
        report.subject = subjectId;
        report.indent(2);
        report.add(damageValue);
        report.add(infernoMissiles);
        vPhaseReport.addElement(report);

        return hits;
    }

    @Override
    protected void handleEntityDamage(Entity entityTarget, Vector<Report> vPhaseReport, IBuilding bldg, int hits,
          int nCluster, int bldgAbsorbs) {
        // Inferno missiles replace the platoon's damage rather than adding to it, so the normal damage
        // application is deliberately skipped. Deliver on the first grouping only: the missile count is for the
        // whole attack, and this method runs once per grouping.
        if (!firstHit) {
            return;
        }

        if (infernoMissiles < 1) {
            Report report = new Report(3332);
            report.subject = subjectId;
            report.indent(2);
            report.add(damageValue);
            vPhaseReport.addElement(report);
            return;
        }

        vPhaseReport.addAll(gameManager.deliverInfernoMissiles(attackingEntity, target, infernoMissiles,
              weapon.getCalledShot().getCall()));
    }
}

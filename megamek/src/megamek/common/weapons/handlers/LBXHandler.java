/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2007-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.handlers;

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
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Andrew Hunter Created on Oct 15, 2004
 */
public class LBXHandler extends AmmoWeaponHandler {
    @Serial
    private static final long serialVersionUID = 6803847280685526644L;

    /**
     *
     */
    public LBXHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) throws EntityLoadingException {
        super(t, w, g, m);
        sSalvoType = " pellet(s) ";
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.WeaponHandler#calcDamagePerHit()
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

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.WeaponHandler#calcHits(Vector<Report>
     * vPhaseReport)
     */
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        // conventional infantry gets hit in one lump
        // BAs can't mount LBXs
        if (target.isConventionalInfantry()) {
            return 1;
        }

        int shotsHit;
        int nHitsModifier = getClusterModifiers(true);

        if (allShotsHit()) {
            shotsHit = weaponType.getRackSize();
            if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RANGE)
                  && (nRange > weaponType.getRanges(weapon)[RangeType.RANGE_LONG])) {
                shotsHit = (int) Math.ceil(shotsHit * .75);
            }
            if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS_RANGE)
                  && (nRange > weaponType.getRanges(weapon)[RangeType.RANGE_EXTREME])) {
                shotsHit = (int) Math.ceil(shotsHit * .5);
            }
        } else {
            PlanetaryConditions conditions = game.getPlanetaryConditions();
            shotsHit = Compute.missilesHit(weaponType.getRackSize(), nHitsModifier, conditions.getEMI().isEMI());
        }

        Report report = new Report(3325);
        report.subject = subjectId;
        report.add(shotsHit);
        report.add(sSalvoType);
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
        bSalvo = true;
        return shotsHit;
    }

    @Override
    protected boolean usesClusterTable() {
        return ammo.getType().getMunitionType().contains(AmmoType.Munitions.M_CLUSTER);
    }

}

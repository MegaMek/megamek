/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.Compute;
import megamek.common.CriticalSlot;
import megamek.common.Game;
import megamek.common.Infantry;
import megamek.common.Mounted;
import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.server.GameManager;
import megamek.server.Server;

public class HyperLaserHandler extends EnergyWeaponHandler {
    private static final long serialVersionUID = 1;

    /**
     * @param toHit
     * @param waa
     * @param g
     */
    public HyperLaserHandler(ToHitData toHit,
            WeaponAttackAction waa, Game g, GameManager m) {
        super(toHit, waa, g, m);
    }

    @Override
    protected boolean doChecks(Vector<Report> vPhaseReport) {
        if (roll <= 3) {
            Report r = new Report(3162);

            r.subject = subjectId;
            r.newlines = 1;
            weapon.setHit(true);
            int wloc = weapon.getLocation();
            for (int i = 0; i < ae.getNumberOfCriticals(wloc); i++) {
                CriticalSlot slot1 = ae.getCritical(wloc, i);
                if ((slot1 == null) || 
                        (slot1.getType() == CriticalSlot.TYPE_SYSTEM)) {
                    continue;
                }
                Mounted mounted = slot1.getMount();
                if (mounted.equals(weapon)) {
                    ae.hitAllCriticals(wloc, i);
                    break;
                }
            }
            r.choose(false);
            vPhaseReport.addElement(r);
            vPhaseReport.addAll(gameManager.explodeEquipment(ae, wloc, weapon));
            return true;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        int[] nRanges = wtype.getRanges(weapon);
        double toReturn = wtype.getDamage(nRange);

        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_ENERGY_WEAPONS)
            && wtype.hasModes()) {
            toReturn = Compute.dialDownDamage(weapon, wtype, nRange);
        }

        // Check for Altered Damage from Energy Weapons (TacOp, pg.83)
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_ALTDMG)) {
            if (nRange <= 1) {
                toReturn++;
            } else if (nRange <= wtype.getMediumRange()) {
                // Do Nothing for Short and Medium Range
            } else if (nRange <= wtype.getLongRange()) {
                toReturn--;
            }
        }

        if (target.isConventionalInfantry()) {
            toReturn = Compute.directBlowInfantryDamage(toReturn,
                    bDirect ? toHit.getMoS() / 3 : 0,
                    wtype.getInfantryDamageClass(),
                    ((Infantry) target).isMechanized(),
                    toHit.getThruBldg() != null, ae.getId(), calcDmgPerHitReport);
            if (nRange <= nRanges[RangeType.RANGE_SHORT]) {
                toReturn += 3;
            } else if (nRange <= nRanges[RangeType.RANGE_MEDIUM]) {
                toReturn += 2;
            } else {
                toReturn++;
            }
        } else if (bDirect) {
            toReturn = Math.min(toReturn + (toHit.getMoS() / 3), toReturn * 2);
        }

        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE)
            && (nRange > nRanges[RangeType.RANGE_LONG])) {
            // Against conventional infantry, treat as direct fire energy
            if (target.isConventionalInfantry()) {
                toReturn -= 1;
            } else { // Else, treat as pulse weapon
                toReturn = (int) Math.floor(toReturn / 2.0);
            }
        }
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_LOS_RANGE)
                && (nRange > nRanges[RangeType.RANGE_EXTREME])) {
         // Against conventional infantry, treat as direct fire energy
            if (target.isConventionalInfantry()) {
                toReturn = (int) Math.floor(toReturn / 2.0);
            } else { // Else, treat as pulse weapon
                toReturn = (int) Math.floor(toReturn / 3.0);
            }

        }

        toReturn = applyGlancingBlowModifier(toReturn, target.isConventionalInfantry());
        return (int) toReturn;
    }

}

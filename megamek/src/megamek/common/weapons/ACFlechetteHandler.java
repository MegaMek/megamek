/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.Building;
import megamek.common.Game;
import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.TargetRoll;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.server.GameManager;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 * @since Sep 25, 2004
 */
public class ACFlechetteHandler extends AmmoWeaponHandler {
    private static final long serialVersionUID = 7965585014230084304L;

    /**
     * @param t
     * @param w
     * @param g
     */
    public ACFlechetteHandler(ToHitData t, WeaponAttackAction w, Game g,
                              GameManager m) {
        super(t, w, g, m);
        damageType = DamageType.FLECHETTE;
    }

    /**
     * Calculate the damage per hit.
     *
     * @return an <code>int</code> representing the damage dealt per hit.
     */
    @Override
    protected int calcDamagePerHit() {
        double toReturn = wtype.getDamage();

        if (bDirect) {
            toReturn += toHit.getMoS() / 3;
        }
        
        toReturn = applyGlancingBlowModifier(toReturn, false);

        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE)
            && (nRange > wtype.getRanges(weapon)[RangeType.RANGE_LONG])) {
            toReturn = (int) Math.floor(toReturn * .75);
        }
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_LOS_RANGE)
                && (nRange > wtype.getRanges(weapon)[RangeType.RANGE_EXTREME])) {
            toReturn = (int) Math.floor(toReturn * .5);
        }        

        return (int) Math.ceil(toReturn);
    }

    @Override
    protected void handleClearDamage(Vector<Report> vPhaseReport, Building bldg, int nDamage) {
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
                && gameManager.tryIgniteHex(target.getPosition(), subjectId, false, false,
                        new TargetRoll(wtype.getFireTN(), wtype.getName()), 5, vPhaseReport)) {
            return;
        }

        Vector<Report> clearReports = gameManager.tryClearHex(target.getPosition(), nDamage, subjectId);
        if (!clearReports.isEmpty()) {
            vPhaseReport.lastElement().newlines = 0;
        }
        vPhaseReport.addAll(clearReports);
    }
}

/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
/*
 * Created on Sep 25, 2004
 *
 */
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.Building;
import megamek.common.IGame;
import megamek.common.RangeType;
import megamek.common.TargetRoll;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.common.server.Report;
import megamek.server.Server;
import megamek.server.Server.DamageType;

/**
 * @author Andrew Hunter
 */
public class ACFlechetteHandler extends AmmoWeaponHandler {
    /**
     *
     */
    private static final long serialVersionUID = 7965585014230084304L;

    /**
     * @param t
     * @param w
     * @param g
     */
    public ACFlechetteHandler(ToHitData t, WeaponAttackAction w, IGame g,
                              Server s) {
        super(t, w, g, s);
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
        } else if (bGlancing) {
            toReturn = (int) Math.floor(toReturn / 2.0);
        }

        if (game.getOptions().booleanOption(OptionsConstants.AC_TAC_OPS_RANGE)
            && (nRange > wtype.getRanges(weapon)[RangeType.RANGE_LONG])) {
            toReturn = (int) Math.floor(toReturn * .75);
        }
        if (game.getOptions().booleanOption(OptionsConstants.AC_TAC_OPS_LOS_RANGE)
                && (nRange > wtype.getRanges(weapon)[RangeType.RANGE_EXTREME])) {
            toReturn = (int) Math.floor(toReturn * .5);
        }        

        return (int) Math.ceil(toReturn);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.weapons.WeaponHandler#handleClearDamage(java.util.Vector,
     * megamek.common.Building, int, boolean)
     */
    @Override
    protected void handleClearDamage(Vector<Report> vPhaseReport,
                                     Building bldg, int nDamage) {
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
            && server.tryIgniteHex(target.getPosition(), subjectId, false,
                                   false,
                                   new TargetRoll(wtype.getFireTN(), wtype.getName()), 5,
                                   vPhaseReport)) {
            return;
        }

        Vector<Report> clearReports = server.tryClearHex(target.getPosition(),
                                                         nDamage, subjectId);
        if (clearReports.size() > 0) {
            vPhaseReport.lastElement().newlines = 0;
        }
        vPhaseReport.addAll(clearReports);
        return;
    }
}

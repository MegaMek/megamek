/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons;

import java.util.Enumeration;
import java.util.Vector;

import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Minefield;
import megamek.common.Report;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 *
 */
public class MRMHandler extends MissileWeaponHandler {
    
    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public MRMHandler(ToHitData t, WeaponAttackAction w, IGame g,
            Server s) {
        super(t, w, g, s);
    }
    
    /*
     *  (non-Javadoc)
     * @see megamek.common.weapons.WeaponHandler#calcHits(java.util.Vector)
     */
    protected int calcHits(Vector vPhaseReport) {
        int missilesHit;
        int nGlancing = 0;
        int nMissilesModifier = 0;
        boolean maxtechmissiles = game.getOptions().booleanOption("maxtech_mslhitpen");
        if (maxtechmissiles) {
            if (nRange<=1) {
                nMissilesModifier += 1;
            } else if (nRange <= wtype.getShortRange()) {
                nMissilesModifier += 0;
            } else if (nRange <= wtype.getMediumRange()) {
                nMissilesModifier -= 1;
            } else {
                nMissilesModifier -= 2;
            }
        }
        if (bGlancing) {
            nGlancing -=4;
        }
        // Large MRM Launcher roll twice
        if ( wtype.getRackSize() == 30 || wtype.getRackSize() == 40 ) {
            missilesHit = Compute.missilesHit(wtype.getRackSize() / 2, nMissilesModifier+nGlancing, maxtechmissiles | bGlancing) +
                Compute.missilesHit(wtype.getRackSize() / 2, nMissilesModifier+nGlancing, maxtechmissiles | bGlancing);
        } else {
            missilesHit = Compute.missilesHit(wtype.getRackSize(), nGlancing + nMissilesModifier , bGlancing | maxtechmissiles);
        }
        int newMissilesHit = missilesHit - getAMSShotDown(vPhaseReport);
        if (amsShotDownTotal > 0) {
            for (int i=0; i < amsShotDown.length; i++) {
                int shotDown = Math.min(amsShotDown[i], missilesHit);
                r = new Report(3350);
                r.indent();
                r.subject = subjectId;
                r.add(amsShotDown[i]);
                r.add(shotDown);
                r.newlines = 0;
                vPhaseReport.addElement(r);
            }
            Report.addNewline(vPhaseReport);
            if (newMissilesHit < 1) {
                //all missiles shot down
                r = new Report(3355);
                r.indent();
                r.subject = subjectId;
                r.newlines = 0;
                vPhaseReport.addElement(r);
            } else {
                r = new Report(3360);
                r.indent();
                r.subject = subjectId;
                r.add(newMissilesHit);
                r.newlines = 0;
                vPhaseReport.addElement(r);
            }
        }
        if (newMissilesHit > 0) {
            r = new Report(3325);
            r.subject = subjectId;
            r.add(newMissilesHit);
            r.add(sSalvoType);
            r.add(toHit.getTableDesc());
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }
        r = new Report(3345);
        r.newlines = 0;
        vPhaseReport.addElement(r);
        bSalvo = true;
        return newMissilesHit;
    }
    
    /*
     *  (non-Javadoc)
     * @see megamek.common.weapons.WeaponHandler#specialResolution(java.util.Vector, megamek.common.Entity, boolean)
     */
    protected boolean specialResolution(Vector vPhaseReport, Entity entityTarget,boolean bMissed) {
        if (!bMissed && target.getTargetType() == Targetable.TYPE_MINEFIELD_CLEAR) {
            int clearAttempt = Compute.d6(2);
            if (clearAttempt >= Minefield.CLEAR_NUMBER_WEAPON) {
                //minefield cleared
                r = new Report(3255);
                r.indent(1);
                r.subject = subjectId;
                vPhaseReport.addElement(r);
                Coords coords = target.getPosition();

                Enumeration minefields = game.getMinefields(coords).elements();
                while (minefields.hasMoreElements()) {
                    Minefield mf = (Minefield) minefields.nextElement();

                    server.removeMinefield(mf);
                }
            } else {
                //fails to clear
                r = new Report(3260);
                r.indent(1);
                r.subject = subjectId;
                vPhaseReport.addElement(r);
            }
        }
        return false;
    }
}

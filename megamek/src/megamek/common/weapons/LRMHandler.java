/**
 * MegaMek - Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
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
 */
public class LRMHandler extends MissileWeaponHandler {

    /**
     * 
     */
    private static final long serialVersionUID = -9160255801810263821L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public LRMHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#specialResolution(java.util.Vector,
     *      megamek.common.Entity, boolean)
     */
    protected boolean specialResolution(Vector<Report> vPhaseReport,
            Entity entityTarget, boolean bMissed) {
        if (!bMissed
                && target.getTargetType() == Targetable.TYPE_MINEFIELD_CLEAR) {
            int clearAttempt = Compute.d6(2);
            if (clearAttempt >= Minefield.CLEAR_NUMBER_WEAPON) {
                // minefield cleared
                r = new Report(3255);
                r.indent(1);
                r.subject = subjectId;
                vPhaseReport.addElement(r);
                Coords coords = target.getPosition();

                Enumeration<Minefield> minefields = game.getMinefields(coords)
                        .elements();
                while (minefields.hasMoreElements()) {
                    Minefield mf = minefields.nextElement();

                    server.removeMinefield(mf);
                }
            } else {
                // fails to clear
                r = new Report(3260);
                r.indent(1);
                r.subject = subjectId;
                vPhaseReport.addElement(r);
            }
        }
        return false;
    }
}

/**
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 29, 2004
 *
 */
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.Game;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 */
public class PrototypeCLUltraWeaponHandler extends UltraWeaponHandler {

    /**
     *
     */
    private static final long serialVersionUID = -3509611454455522296L;

    /**
     * @param t
     * @param w
     * @param g
     */
    public PrototypeCLUltraWeaponHandler(ToHitData t, WeaponAttackAction w,
            Game g, Server s) {
        super(t, w, g, s);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.UltraWeaponHandler#doChecks(java.util.Vector)
     */
    @Override
    protected boolean doChecks(Vector<Report> vPhaseReport) {
        if (doAmmoFeedProblemCheck(vPhaseReport)) {
            return true;
        }
        
        if ((roll <= 3) && (howManyShots == 2)) {
            Report r = new Report(3160);
            r.subject = subjectId;
            weapon.setJammed(true);
            weapon.setHit(true);
            vPhaseReport.addElement(r);
            return true;
        }
        return false;
    }
}

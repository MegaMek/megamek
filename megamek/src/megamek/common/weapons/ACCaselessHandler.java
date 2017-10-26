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
 * Created on Oct 15, 2017
 *
 */
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.CriticalSlot;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Mounted;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Dave Nawton
 */


public class ACCaselessHandler extends ACWeaponHandler {

    private static final long serialVersionUID = -6614562346449113878L;

    /**
     * @param t
     * @param w
     * @param g
     */
    public ACCaselessHandler (ToHitData t, WeaponAttackAction w,
            IGame g, Server s) {
        super(t, w, g, s);
    }

    @Override
    protected boolean doChecks(Vector<Report> vPhaseReport) {
        if ((roll <= 2) && !(ae instanceof Infantry)) {
            Report r = new Report(3161);
            r.subject = subjectId;
            weapon.setJammed(true);
            weapon.setHit(true);
            weapon.setFired(false);
            
            int wloc = weapon.getLocation();
            for (int i = 0; i < ae.getNumberOfCriticals(wloc); i++) {
                CriticalSlot slot1 = ae.getCritical(wloc, i);
                if ((slot1 == null) || (slot1.getType() == CriticalSlot.TYPE_SYSTEM)) {
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
        }

        return false;
    }
}


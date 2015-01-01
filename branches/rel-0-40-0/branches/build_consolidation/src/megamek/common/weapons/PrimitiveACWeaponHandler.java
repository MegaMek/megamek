/**
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005,2006,2007 Ben Mazur (bmazur@sev.org)
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

import megamek.common.IGame;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

import java.util.Vector;

/**
 * Deric Page (deric.page@usa.net)
 */
public class PrimitiveACWeaponHandler extends ACWeaponHandler {
    /**
     * 
     */
    private static final long serialVersionUID = -3686194077871525280L;

    /**
     * @param t
     * @param w
     * @param g
     */
    public PrimitiveACWeaponHandler(ToHitData t, WeaponAttackAction w, IGame g,
            Server s) {
        super(t, w, g, s);
    }

    @Override
    protected boolean doChecks(Vector<Report> vPhaseReport) {
        if (roll == 2) {
            Report r = new Report(3161);
            r.subject = subjectId;
            r.newlines = 0;
            vPhaseReport.addElement(r);
            weapon.setJammed(true);
            weapon.setHit(true);
        }
        return false;
    }
}

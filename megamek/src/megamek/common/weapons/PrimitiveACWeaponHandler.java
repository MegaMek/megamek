/*
 * MegaMek -
 * Copyright (C) 2000-2007 Ben Mazur (bmazur@sev.org)
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

import megamek.common.Game;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.GameManager;

/**
 * Deric "Netzilla" Page (deric dot page at usa dot net)
 */
public class PrimitiveACWeaponHandler extends ACWeaponHandler {
    private static final long serialVersionUID = -3686194077871525280L;

    /**
     * @param t
     * @param w
     * @param g
     */
    public PrimitiveACWeaponHandler(ToHitData t, WeaponAttackAction w, Game g, GameManager m) {
        super(t, w, g, m);
    }

    @Override
    protected boolean doChecks(Vector<Report> vPhaseReport) {
        if (doAmmoFeedProblemCheck(vPhaseReport)) {
            return true;
        }
        
        if (roll.getIntValue() == 2) {
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

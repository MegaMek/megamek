/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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

import megamek.common.AmmoType;
import megamek.common.Game;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.gameManager.GameManager;
import megamek.server.Server;

import java.util.Vector;

/**
 * @author Andrew Hunter
 * @since Sept 29, 2004
 */
public class PrototypeISUltraWeaponHandler extends UltraWeaponHandler {
    private static final long serialVersionUID = 6441106275439235564L;

    /**
     * @param t
     * @param w
     * @param g
     */
    public PrototypeISUltraWeaponHandler(ToHitData t, WeaponAttackAction w, Game g, GameManager m) {
        super(t, w, g, m);
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
        
        if (((roll.getIntValue() <= 4) && (howManyShots == 2))
                || ((roll.getIntValue() == 2) && (howManyShots == 1))) {
            Report r = new Report();
            r.subject = subjectId;
            weapon.setJammed(true);
            if (wtype.getAmmoType() == AmmoType.T_AC_ULTRA) {
                r.messageId = 3160;
                weapon.setHit(true);
            } else {
                r.messageId = 3170;
            }
            vPhaseReport.addElement(r);
            return true;
        }
        return false;
    }
}

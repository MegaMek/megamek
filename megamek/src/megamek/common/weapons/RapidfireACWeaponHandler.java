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

import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 */
public class RapidfireACWeaponHandler extends UltraWeaponHandler {
    /**
     *
     */
    private static final long serialVersionUID = -1770392652874842106L;

    /**
     * @param t
     * @param w
     * @param g
     */
    public RapidfireACWeaponHandler(ToHitData t, WeaponAttackAction w, IGame g,
            Server s) {
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
        
        int jamLevel = 4;
        boolean kindRapidFire = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_KIND_RAPID_AC);
        if (kindRapidFire) {
            jamLevel = 2;
        }
        if ((roll <= jamLevel) && (howManyShots == 2) && !(ae instanceof Infantry)) {
            if (roll > 2 || kindRapidFire) {
                Report r = new Report(3161);
                r.subject = subjectId;
                r.newlines = 0;
                vPhaseReport.addElement(r);
                weapon.setJammed(true);
            } else {
                Report r = new Report(3162);
                r.subject = subjectId;
                r.choose(false);
                r.indent();
                vPhaseReport.addElement(r);
                
                explodeRoundInBarrel(vPhaseReport);
            }
            return false;
        }
        return false;
    }

    @Override
    protected boolean usesClusterTable() {
        return true;
    }

    @Override
    protected boolean canDoDirectBlowDamage() {
        return false;
    }

}

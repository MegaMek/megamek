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
/*
 * Created on Oct 15, 2004
 *
 */
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public class PrototypeLBXHandler extends LBXHandler {
    /**
     * 
     */
    private static final long serialVersionUID = -5200908977142584431L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public PrototypeLBXHandler(ToHitData t, WeaponAttackAction w, IGame g,
            Server s) {
        super(t, w, g, s);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcHits(Vector<Report>
     *      vPhaseReport)
     */
    protected int calcHits(Vector<Report> vPhaseReport) {
        // conventional infantry gets hit in one lump
        // BAs can't mount LBXs
        if (target instanceof Infantry && !(target instanceof BattleArmor)) {
            return 1;
        }
        
        int shotMod = -1;
        if (bGlancing) {
            shotMod -= 4;
        }
        if(game.getPlanetaryConditions().hasEMI()) {
            shotMod -= 2;
        }
        int shotsHit = allShotsHit() ? wtype.getRackSize() : Compute
                .missilesHit(wtype.getRackSize(), shotMod);
        
        r = new Report(3325);
        r.subject = subjectId;
        r.add(shotsHit);
        r.add(" shot(s) ");
        r.add(toHit.getTableDesc());
        r.newlines = 0;
        vPhaseReport.addElement(r);
        r = new Report(3345);
        r.subject = subjectId;
        r.newlines = 0;
        vPhaseReport.addElement(r);
        bSalvo = true;
        return shotsHit;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#doChecks(java.util.Vector)
     */
    protected boolean doChecks(Vector<Report> vPhaseReport) {
        if (roll == 2) {
            r = new Report(3165);
            r.subject = subjectId;
            weapon.setJammed(true);
            weapon.setHit(true);
            vPhaseReport.addElement(r);
            return true;
        }
        return false;
    }
}

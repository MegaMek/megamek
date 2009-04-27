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
 * Created on Sep 24, 2004
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
public class InfantryMGHandler extends InfantryWeaponHandler {

    /**
     * 
     */
    private static final long serialVersionUID = -1502160583884886059L;

    /**
     * @param t
     * @param w
     * @param g
     */
    public InfantryMGHandler(ToHitData t, WeaponAttackAction w, IGame g,
            Server s) {
        super(t, w, g, s);
        damage = new int[] { 1, 1, 2, 2, 3, 3, 4, 4, 5, 6, 6, 7, 7, 8, 8, 9,
                10, 10, 11, 11, 12, 12, 13, 13, 14, 15, 15, 16, 16, 17 };
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcHits(java.util.Vector)
     */
    protected int calcHits(Vector<Report> vPhaseReport) {
        // conventional infantry gets an extra d6 damage
        if (target instanceof Infantry && !(target instanceof BattleArmor)) {
            int troopersHit = Compute.missilesHit(((Infantry) ae)
                    .getShootingStrength());
            r = new Report(3325);
            r.subject = subjectId;
            r.add(troopersHit);
            r.add(" troopers ");
            int toReturn = damage[troopersHit - 1] + Compute.d6();
            r.add(toHit.getTableDesc() + ", causing " + toReturn + "damage.");
            r.newlines = 0;
            vPhaseReport.addElement(r);
            return toReturn;
        } else
            return super.calcHits(vPhaseReport);
    }

}

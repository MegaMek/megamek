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

import java.util.Vector;

import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Report;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 *
 */
public class NarcExplosiveHandler extends MissileWeaponHandler {

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public NarcExplosiveHandler(ToHitData t, WeaponAttackAction w, IGame g,
            Server s) {
        super(t, w, g, s);
        sSalvoType = " explosive pod ";        
    }
    
    /*
     *  (non-Javadoc)
     * @see megamek.common.weapons.WeaponHandler#calcHits(java.util.Vector)
     */
    protected int calcHits(Vector vPhaseReport) {
        bSalvo = true;
        Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
                : null;
        getAMSShotDown(vPhaseReport);
        if (amsShotDownTotal > 0) {
            r = new Report(3235);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
            for (int i=0; i < amsShotDown.length; i++) {
                r = new Report(3230);
                r.indent(1);
                r.subject = subjectId;
                r.add(amsShotDown[i]);
                vPhaseReport.addElement(r);
            }
            r = new Report(3240);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
            return 0;
        } else {
            r = new Report(3325);
            r.subject = subjectId;
            r.add(1);
            r.add(sSalvoType);
            r.add(toHit.getTableDesc());
            r.newlines = 0;
            vPhaseReport.addElement(r);
            return 1;
        }
    }
    
    /*
     *  (non-Javadoc)
     * @see megamek.common.weapons.WeaponHandler#calcnCluster()
     */
    protected int calcnCluster() {
        return 1;
    }
    
    /*
     *  (non-Javadoc)
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    protected int calcDamagePerHit() {
        AmmoType atype = (AmmoType)ammo.getType();
        if (atype.getAmmoType() == AmmoType.T_INARC) {
            return 6;
        } else {
            return 4;
        }
        
    }
}

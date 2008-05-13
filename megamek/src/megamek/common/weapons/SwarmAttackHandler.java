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
 * Created on Sep 23, 2004
 *
 */
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.Building;
import megamek.common.Entity;
import megamek.common.HitData;
import megamek.common.IGame;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 */
public class SwarmAttackHandler extends WeaponHandler {
    /**
     * 
     */
    private static final long serialVersionUID = -2439937071168853215L;

    /**
     * @param toHit
     * @param waa
     * @param g
     */
    public SwarmAttackHandler(ToHitData toHit, WeaponAttackAction waa, IGame g,
            Server s) {
        super(toHit, waa, g, s);
        generalDamageType = HitData.DAMAGE_NONE;
    }

    protected void handleEntityDamage(Entity entityTarget,
            Vector<Report> vPhaseReport, Building bldg, int hits, int nCluster,
            int nDamPerHit, int bldgAbsorbs) {
        Report r;
        // Is the target already swarmed?
        if (Entity.NONE != entityTarget.getSwarmAttackerId()) {
            r = new Report(3265);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }
        // Did the target get destroyed by weapons fire?
        else if (entityTarget.isDoomed() || entityTarget.isDestroyed()
                || entityTarget.getCrew().isDead()) {
            r = new Report(3270);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        } else {
            // success
            r = new Report(3275);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
            ae.setSwarmTargetId(waa.getTargetId());
            entityTarget.setSwarmAttackerId(waa.getEntityId());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcHits(java.util.Vector)
     */
    protected int calcHits(Vector<Report> vPhaseReport) {
        return 1;
    }
}

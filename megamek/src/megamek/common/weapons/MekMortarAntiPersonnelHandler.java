/*
 * MegaMek - Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
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

import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.Report;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.GameManager;
import megamek.server.Server;

/**
 * @author arlith
 */
public class MekMortarAntiPersonnelHandler extends AmmoWeaponHandler {
    private static final long serialVersionUID = -2073773899108954657L;
    
    String sSalvoType = " shell(s) ";

    /**
     * @param t
     * @param w
     * @param g
     * @param m
     */
    public MekMortarAntiPersonnelHandler(ToHitData t, WeaponAttackAction w,
            Game g, GameManager m) {
        super(t, w, g, m);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcHits(java.util.Vector)
     */
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        boolean targetHex = (target.getTargetType() == Targetable.TYPE_HEX_CLEAR)
                || (target.getTargetType() == Targetable.TYPE_HEX_IGNITE);
        int missilesHit;
        int nMissilesModifier = getClusterModifiers(true);

        if (targetHex) {
            missilesHit = wtype.getRackSize();
        } else {
            missilesHit = Compute.missilesHit(wtype.getRackSize(),
                    nMissilesModifier);
        }

        if (missilesHit > 0) {
            Report r = new Report(3325);
            r.subject = subjectId;
            r.add(missilesHit);
            r.add(sSalvoType);
            if (target.isConventionalInfantry()) {
                r.add("");
            } else {
                r.add(toHit.getTableDesc());
            }
            r.newlines = 0;
            vPhaseReport.addElement(r);
            if (nMissilesModifier != 0) {
                if (nMissilesModifier > 0) {
                    r = new Report(3340);
                } else {
                    r = new Report(3341);
                }
                r.subject = subjectId;
                r.add(nMissilesModifier);
                r.newlines = 0;
                vPhaseReport.addElement(r);
            }
        }
        Report r = new Report(3345);
        r.subject = subjectId;
        vPhaseReport.addElement(r);
        bSalvo = true;
        return missilesHit;
    }

    /**
     * Calculate the clustering of the hits
     * 
     * @return a <code>int</code> value saying how much hits are in each cluster
     *         of damage.
     */
    @Override
    protected int calcnCluster() {
        return 1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        if (target.isConventionalInfantry()) {
            int damage;
            int numDice = 1;
            if (bDirect) {
                numDice += (toHit.getMoS() / 3);
            }
            damage = Compute.d6(numDice);
            damage = applyGlancingBlowModifier(damage, true);
            // Burst fire damage rounds up
            return (int) Math.ceil(damage);
        }
        return 1;
    }
    
    @Override
    protected void handleEntityDamage(Entity entityTarget, Vector<Report> vPhaseReport,
                                      Building bldg, int hits, int nCluster, int bldgAbsorbs) {
        super.handleEntityDamage(entityTarget, vPhaseReport, bldg, hits,
                nCluster, bldgAbsorbs);
        
        // We need to roll damage for each hit against infantry
        if (target.isConventionalInfantry()) {
            nDamPerHit = calcDamagePerHit();
        }
    }

}

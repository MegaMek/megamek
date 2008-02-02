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

import megamek.common.BattleArmor;
import megamek.common.Building;
import megamek.common.Entity;
import megamek.common.HitData;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;
import megamek.server.Server.DamageType;

/**
 * @author Sebastian Brocks
 *
 */
public class SRMFragHandler extends SRMHandler {

    /**
     * 
     */
    private static final long serialVersionUID = -2281133981582906299L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public SRMFragHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
        sSalvoType = " fragmentation missile(s) ";
    }
    
    
    /**
     * Calculate the damage per hit.
     * 
     * @return an <code>int</code> representing the damage dealt per hit.
     */
    protected int calcDamagePerHit() {
        float toReturn = wtype.getDamage();
        // during a swarm, all damage gets applied as one block to one location
        if (ae instanceof BattleArmor && !wtype.hasFlag(WeaponType.F_BATTLEARMOR)
                && (ae.getSwarmTargetId() == target.getTargetId())) {
            toReturn *= ((BattleArmor)ae).getShootingStrength();
        }

        if ( !(target instanceof Infantry) )
            toReturn = 0;
        
        if (bGlancing) {
            toReturn/=2;
        }
        return (int)Math.ceil(toReturn);
    }


    /*
     *  (non-Javadoc)
     * @see megamek.common.weapons.WeaponHandler#handleBuildingDamage(java.util.Vector, megamek.common.Building, int, boolean)
     */
    protected void handleClearDamage(Vector<Report> vPhaseReport, Building bldg,
            int nDamage, boolean bSalvo) {
        if (!bSalvo) {
            // hits!
            r = new Report(2270);
            r.subject = subjectId;
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }
        // report that damage was "applied" to terrain
        
        //Fragmentation does double damage to woods
        nDamage *= 2;
        
        r = new Report(3385);
        r.indent();
        r.subject = subjectId;
        r.add(nDamage);
        vPhaseReport.addElement(r);

        // Any clear attempt can result in accidental ignition, even
        // weapons that can't normally start fires. that's weird.
        // Buildings can't be accidentally ignited.
        if (bldg != null
                && server.tryIgniteHex(target.getPosition(), subjectId, false,
                        9)) {
            return;
        }

        //int tn = 14 - nDamage;
        server.tryClearHex(target.getPosition(), nDamage, subjectId);
        return;
    }


    /*
     *  (non-Javadoc)
     * @see megamek.common.weapons.WeaponHandler#handleBuildingDamage(java.util.Vector, megamek.common.Building, int, boolean)
     */
    protected void handleBuildingDamage(Vector<Report> vPhaseReport, Building bldg,
            int nDamage, boolean bSalvo) {
        return;
    }

    /*
     *  (non-Javadoc)
     * @see megamek.common.weapons.WeaponHandler#handleEntityDamage(megamek.common.Entity, java.util.Vector, megamek.common.Building, int, int, int, int)
     */
    protected void handleEntityDamage(Entity entityTarget,
            Vector<Report> vPhaseReport, Building bldg, int hits, int nCluster,
            int nDamPerHit, int bldgAbsorbs) {
        int nDamage;
        HitData hit = entityTarget.rollHitLocation(toHit.getHitTable(), toHit
                .getSideTable(), waa.getAimedLocation(), waa.getAimingMode());
        // Resolve damage normally.
        nDamage = nDamPerHit * Math.min(nCluster, hits);

        // A building may be damaged, even if the squad is not.
        if ( bldgAbsorbs > 0 ) {
            int toBldg = Math.min( bldgAbsorbs, nDamage );
            nDamage -= toBldg;
            Report.addNewline(vPhaseReport);
            Vector<Report> buildingReport = server.damageBuilding( bldg, toBldg );
            for (Report report: buildingReport) {
                report.subject = subjectId;
            }
            vPhaseReport.addAll(buildingReport);
        }
        if (bGlancing) {
            hit.makeGlancingBlow();
        }
        // A building may absorb the entire shot.
        if ( nDamage == 0 ) {
            r = new Report(3415);
            r.subject = subjectId;
            r.indent(2);
            r.addDesc(entityTarget);
            r.newlines = 0;
            vPhaseReport.addElement(r);
        } else {
            if (bGlancing) {
                hit.makeGlancingBlow();
            }
            vPhaseReport.addAll(
                    server.damageEntity(entityTarget, hit, nDamage, false, DamageType.FRAGMENTATION, false, false, throughFront));
        }

    }
}

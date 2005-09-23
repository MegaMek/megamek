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
 * Created on Sep 23, 2004
 *
 */
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 * 
 */
public class BAWeaponHandler extends WeaponHandler {

    String sSalvoType;

    /**
     * @param toHit
     * @param waa
     * @param g
     */
    public BAWeaponHandler(ToHitData toHit, WeaponAttackAction waa, IGame g,
            Server s) {
        super(toHit, waa, g, s);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcHits(java.util.Vector)
     */
    protected int calcHits(Vector vPhaseReport) {
        Infantry platoon = (Infantry) ae;
        int hits = platoon.getShootingStrength();
        // All attacks during Mek Swarms hit; all
        // others use the Battle Armor hits table.
        if (!allShotsHit()) {
            hits = Compute.getBattleArmorHits(hits);
        }
        // If we're swarming, add vibro-claw damage.
        if (ae.getSwarmTargetId() == waa.getTargetId()) {
            nDamPerHit += ((BattleArmor) ae).getVibroClawDamage();
        }
        bSalvo = true;
        sSalvoType = " shot(s) ";
        r = new Report(3325);
        r.subject = subjectId;
        r.add(hits);
        r.add(sSalvoType);
        r.add(toHit.getTableDesc());
        r.newlines = 0;
        vPhaseReport.addElement(r);
        r = new Report(3345);
        r.newlines = 0;
        vPhaseReport.addElement(r);
        return hits;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#allShotsHit()
     */
    protected boolean allShotsHit() {
        return ae.getSwarmTargetId() == waa.getTargetId();
    }

    /*
     *  (non-Javadoc)
     * @see megamek.common.weapons.WeaponHandler#handleEntityDamage(megamek.common.Entity, java.util.Vector, megamek.common.Building, int, int, int, int)
     */
    protected void handleEntityDamage(Entity entityTarget, Vector vPhaseReport,
            Building bldg, int hits, int nCluster, int nDamPerHit,
            int bldgAbsorbs) {
        if (wtype.getAmmoType() == AmmoType.T_BA_MG
                && (target instanceof Infantry)
                && !(target instanceof BattleArmor)) {

            // ASSUMPTION: Building walls protect infantry from BA MGs.
            if (bldgAbsorbs > 0) {
                int toBldg = nDamPerHit * hits;
                r = new Report(3295);
                r.newlines = 0;
                r.subject = subjectId;
                r.add(hits);
                r.add(sSalvoType);
                vPhaseReport.addElement(r);

                Report buildingReport = server.damageBuilding(bldg, Math.min(
                        toBldg, bldgAbsorbs), " absorbs the shots, taking ");
                buildingReport.newlines = 1;
                buildingReport.subject = subjectId;
                vPhaseReport.addElement(buildingReport);
                return;
            }
            nDamPerHit = Compute.d6(hits);
            r = new Report(3300);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(nDamPerHit);
            r.add(sSalvoType);
            vPhaseReport.addElement(r);
            hits = 1;
        }
        super.handleEntityDamage(entityTarget, vPhaseReport, bldg, hits,
                nCluster, nDamPerHit, bldgAbsorbs);
    }
    
    /*
     *  (non-Javadoc)
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    protected int calcDamagePerHit() {
        if (bGlancing) {
            return (int)Math.floor(wtype.getRackSize()/2.0);
        }
        return wtype.getRackSize();
    }
}

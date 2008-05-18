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
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.HitData;
import megamek.common.IGame;
import megamek.common.INarcPod;
import megamek.common.NarcPod;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public class NarcHandler extends MissileWeaponHandler {

    /**
     * 
     */
    private static final long serialVersionUID = 3195613885543781820L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public NarcHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcHits(java.util.Vector)
     */
    protected int calcHits(Vector<Report> vPhaseReport) {
        bSalvo = true;
        if (amsEnganged) {
            r = new Report(3235);
            r.subject = subjectId;
            vPhaseReport.add(r);
            r = new Report(3230);
            r.indent(1);
            r.subject = subjectId;
            vPhaseReport.add(r);
            int destroyRoll = Compute.d6();
            if (destroyRoll <= 3) {
                r = new Report(3240);
                r.subject = subjectId;
                r.add(destroyRoll);
                vPhaseReport.add(r);
                return 0;
            }
            r = new Report(3241);
            r.add(destroyRoll);
            r.subject = subjectId;
            vPhaseReport.add(r);
        }
        return 1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcnCluster()
     */
    protected int calcnCluster() {
        return 1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    protected int calcDamagePerHit() {
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#handleEntityDamage(megamek.common.Entity,
     *      java.util.Vector, megamek.common.Building, int, int, int, int)
     */
    protected void handleEntityDamage(Entity entityTarget,
            Vector<Report> vPhaseReport, Building bldg, int hits, int nCluster,
            int nDamPerHit, int bldgAbsorbs) {
        HitData hit = entityTarget.rollHitLocation(toHit.getHitTable(), toHit
                .getSideTable(), waa.getAimedLocation(), waa.getAimingMode());
        AmmoType atype = (AmmoType) ammo.getType();
        if (atype.getAmmoType() == AmmoType.T_NARC) {
            // narced
            NarcPod pod = new NarcPod(ae.getOwner().getTeam(), hit
                    .getLocation());
            r = new Report(3250);
            r.subject = subjectId;
            r.add(entityTarget.getDisplayName());
            r.add(entityTarget.getLocationAbbr(hit));
            vPhaseReport.addElement(r);
            entityTarget.attachNarcPod(pod);
        } else if (atype.getAmmoType() == AmmoType.T_INARC) {
            // iNarced
            INarcPod pod = null;
            if (atype.getMunitionType() == AmmoType.M_ECM) {
                pod = new INarcPod(ae.getOwner().getTeam(), INarcPod.ECM, hit
                        .getLocation());
                r = new Report(3251);
                r.subject = subjectId;
                r.add(entityTarget.getDisplayName());
                r.add(entityTarget.getLocationAbbr(hit));
                vPhaseReport.addElement(r);
            } else if (atype.getMunitionType() == AmmoType.M_HAYWIRE) {
                pod = new INarcPod(ae.getOwner().getTeam(), INarcPod.HAYWIRE,
                        hit.getLocation());
                r = new Report(3252);
                r.subject = subjectId;
                r.add(entityTarget.getDisplayName());
                r.add(entityTarget.getLocationAbbr(hit));
                vPhaseReport.addElement(r);
            } else if (atype.getMunitionType() == AmmoType.M_NEMESIS) {
                pod = new INarcPod(ae.getOwner().getTeam(), INarcPod.NEMESIS,
                        hit.getLocation());
                r = new Report(3253);
                r.add(entityTarget.getDisplayName());
                r.add(entityTarget.getLocationAbbr(hit));
                r.subject = subjectId;
                vPhaseReport.addElement(r);
            } else {
                pod = new INarcPod(ae.getOwner().getTeam(), INarcPod.HOMING,
                        hit.getLocation());
                r = new Report(3254);
                r.subject = subjectId;
                r.add(entityTarget.getDisplayName());
                r.add(entityTarget.getLocationAbbr(hit));
                vPhaseReport.addElement(r);
            }
            entityTarget.attachINarcPod(pod);
        }
    }
}

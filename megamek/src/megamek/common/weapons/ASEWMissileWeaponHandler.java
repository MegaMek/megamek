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

import megamek.common.Aero;
import megamek.common.Building;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.LandAirMech;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author MKerensky
 */
public class ASEWMissileWeaponHandler extends ThunderBoltWeaponHandler {

    /**
     *
     */
    private static final long serialVersionUID = 6359291710822171023L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public ASEWMissileWeaponHandler(ToHitData t, WeaponAttackAction w, IGame g,
            Server s) {
        super(t, w, g, s);
    }
    
    boolean badTarget = false;

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.weapons.WeaponHandler#handleEntityDamage(megamek.common
     * .Entity, java.util.Vector, megamek.common.Building, int, int, int, int)
     */
    @Override
    protected void handleEntityDamage(Entity entityTarget,
            Vector<Report> vPhaseReport, Building bldg, int hits, int nCluster,
            int bldgAbsorbs) {
        missed = false;

        hit = entityTarget.rollHitLocation(toHit.getHitTable(),
                toHit.getSideTable(), waa.getAimedLocation(),
                waa.getAimingMode(), toHit.getCover());
        hit.setGeneralDamageType(generalDamageType);
        hit.setCapital(wtype.isCapital());
        hit.setBoxCars(roll == 12);
        hit.setCapMisCritMod(getCapMisMod());
        hit.setFirstHit(firstHit);
        hit.setAttackerId(getAttackerId());
        if (weapon.isWeaponGroup()) {
            hit.setSingleAV(attackValue);
        }
        Report r = new Report(3405);
        r.subject = subjectId;
        r.add(toHit.getTableDesc());
        r.add(entityTarget.getLocationAbbr(hit));
        vPhaseReport.addElement(r);
        if (nweaponsHit > 1) {
            r.newlines = 1;
            r = new Report(3471);
            r.subject = subjectId;
            r.addDesc(entityTarget);
            r.add(nweaponsHit);
            vPhaseReport.add(r);
        } else {
            r.newlines = 1;
            r = new Report(3470);
            r.subject = subjectId;
            r.addDesc(entityTarget);
            vPhaseReport.add(r); 
        }
        //Large craft suffer a to-hit penalty for the location struck. Other units just suffer a flat +4 penalty until the effects expire
        if ((entityTarget instanceof Aero) 
                || (entityTarget instanceof LandAirMech 
                        && entityTarget.getConversionMode() == LandAirMech.CONV_MODE_FIGHTER)) {
            Aero a = (Aero) entityTarget;
            int loc = hit.getLocation();
            if (a.getASEWAffected(loc) > 0) {
                a.setASEWAffected(loc, (a.getASEWAffected(loc) + nweaponsHit));
                r = new Report(3473);
                r.subject = subjectId;
                r.add(entityTarget.getLocationAbbr(hit));
                r.add(nweaponsHit);
                vPhaseReport.add(r); 
            } else {
                a.setASEWAffected(loc, 2);
                r = new Report(3472);
                r.subject = subjectId;
                r.add(entityTarget.getLocationAbbr(hit));
                vPhaseReport.add(r); 
            }
        } else {
            // The rules don't say that you can't hit a mech standing on the hull of a dropship
            // with one of these, but they don't say what it would do if you did, either...
            // We'll assume it has no effect for now, though it probably should do something.
            badTarget = true;
        }
    }
    
    /**
     * Calculate the attack value based on range
     *
     * @return an <code>int</code> representing the attack value at that range.
     */
    @Override
    protected int calcAttackValue() {
        calcCounterAV();
        int av = 0;
        return av;
    }

}

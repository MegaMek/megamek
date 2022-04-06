/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.weapons;

import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

import java.util.Vector;

/**
 * @author MKerensky
 */
public class ASEWMissileWeaponHandler extends ThunderBoltWeaponHandler {
    private static final long serialVersionUID = 6359291710822171023L;

    /**
     * Weapon handler for Anti Ship Electronic Warfare Missiles
     * Single, large missile - behaves like a thunderbolt except for damage.
     * @param t - ToHit roll data
     * @param w - The weapon attack action for this ASEW missile
     * @param g - The current game
     * @param s - The current server instance
     */
    public ASEWMissileWeaponHandler(ToHitData t, WeaponAttackAction w, Game g, Server s) {
        super(t, w, g, s);
    }
    
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
        //Report the hit table and location
        Report r = new Report(3405);
        r.subject = subjectId;
        r.add(toHit.getTableDesc());
        r.add(entityTarget.getLocationAbbr(hit));
        vPhaseReport.addElement(r);
        if (nweaponsHit > 1) {
            //If the target is hit by multiple ASEW missiles, report it here, even if the effects don't stack
            r.newlines = 1;
            r = new Report(3471);
            r.subject = subjectId;
            r.addDesc(entityTarget);
            r.add(nweaponsHit);
            vPhaseReport.add(r);
        } else {
            //Otherwise, report a single ASEW missile hit
            r.newlines = 1;
            r = new Report(3470);
            r.subject = subjectId;
            r.addDesc(entityTarget);
            vPhaseReport.add(r); 
        }
        //Large craft suffer a to-hit penalty for the location struck. 
        if (entityTarget instanceof Dropship) { 
            Dropship d = (Dropship) entityTarget;
            int loc = hit.getLocation();
            d.setASEWAffected(loc, 2);
            //Report the arc affected by the attack and the duration of the effects
            r = new Report(3472);
            r.subject = subjectId;
            r.add(entityTarget.getLocationAbbr(hit));
            vPhaseReport.add(r);             
        } else if (entityTarget instanceof Jumpship) {
            Jumpship j = (Jumpship) entityTarget;
            int loc = hit.getLocation();
            j.setASEWAffected(loc, 2);
            //If a Warship is hit in the fore or aft side, the broadside arc is also affected
            if ((j instanceof Warship) 
                    && (loc == Jumpship.LOC_FLS || loc == Jumpship.LOC_ALS)) {
                j.setASEWAffected(Warship.LOC_LBS, 2);
                //Report the arc hit by the attack and the associated broadside and the duration of the effects
                r = new Report(3474);
                r.subject = subjectId;
                r.add(entityTarget.getLocationAbbr(hit));
                r.add("LBS");
                vPhaseReport.add(r);
            } else if ((j instanceof Warship) 
                    && (loc == Jumpship.LOC_FRS || loc == Jumpship.LOC_ARS)) {
                j.setASEWAffected(Warship.LOC_RBS, 2);
                //Report the arc hit by the attack and the associated broadside and the duration of the effects
                r = new Report(3474);
                r.subject = subjectId;
                r.add(entityTarget.getLocationAbbr(hit));
                r.add("RBS");
                vPhaseReport.add(r);
            } else {
            //If the nose or aft is hit, just report the arc affected by the attack and the duration of the effects
            r = new Report(3472);
            r.subject = subjectId;
            r.add(entityTarget.getLocationAbbr(hit));
            vPhaseReport.add(r);
            }
        } else {
            // Other units just suffer a flat +4 penalty until the effects expire
            entityTarget.setASEWAffected(2);
            //Report the duration of the effects
            r = new Report(3473);
            r.subject = subjectId;
            vPhaseReport.add(r);
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
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
import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public class ThunderBoltWeaponHandler extends MissileWeaponHandler {

    /**
     *
     */
    private static final long serialVersionUID = 6329291710822071023L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public ThunderBoltWeaponHandler(ToHitData t, WeaponAttackAction w, IGame g,
            Server s) {
        super(t, w, g, s);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        AmmoType atype = (AmmoType) ammo.getType();
        double toReturn = atype.getDamagePerShot();
        int minRange;
        if (ae.isAirborne()) {
            minRange = wtype.getATRanges()[RangeType.RANGE_MINIMUM];
        } else {
            minRange = wtype.getMinimumRange();
        }
        if ((nRange <= minRange) && !weapon.isHotLoaded()) {
            toReturn /= 2;
            toReturn = Math.floor(toReturn);
        }
        if ((target instanceof Infantry) && !(target instanceof BattleArmor)) {
            toReturn = Compute.directBlowInfantryDamage(toReturn,
                    bDirect ? toHit.getMoS() / 3 : 0,
                    wtype.getInfantryDamageClass(),
                    ((Infantry) target).isMechanized(),
                    toHit.getThruBldg() != null, ae.getId(), calcDmgPerHitReport);
        } else if (bDirect) {
            toReturn = Math.min(toReturn + (toHit.getMoS() / 3), toReturn * 2);
        }
        return (int) Math.ceil(toReturn);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.weapons.MissileWeaponHandler#calcHits(java.util.Vector)
     */
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        getAMSHitsMod(vPhaseReport);
        bSalvo = true;
        if (amsEngaged) {
            Report r = new Report(3235);
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
                r.add("missile");
                r.add(destroyRoll);
                vPhaseReport.add(r);
                return 0;
            }
            r = new Report(3241);
            r.add("missile");
            r.add(destroyRoll);
            r.subject = subjectId;
            vPhaseReport.add(r);
        }
        return 1;
    }

}

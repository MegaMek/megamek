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

import megamek.common.BattleArmor;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Report;
import megamek.common.Tank;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public class FluidGunCoolHandler extends AmmoWeaponHandler {
    /**
     *
     */
    private static final long serialVersionUID = 4856089237895318515L;

    /**
     * @param toHit
     * @param waa
     * @param g
     */
    public FluidGunCoolHandler(ToHitData toHit, WeaponAttackAction waa,
            IGame g, Server s) {
        super(toHit, waa, g, s);
    }

    @Override
    protected void handleEntityDamage(Entity entityTarget,
            Vector<Report> vPhaseReport, Building bldg, int hits, int nCluster,
            int bldgAbsorbs) {
        if ((entityTarget instanceof Infantry)
                && !(entityTarget instanceof BattleArmor)) {
            // 1 point direct-fire ballistic
            nDamPerHit = Compute.directBlowInfantryDamage(1,
                    bDirect ? toHit.getMoS() / 3 : 0,
                    WeaponType.WEAPON_DIRECT_FIRE,
                    ((Infantry) target).isMechanized(),
                    toHit.getThruBldg() != null);
            super.handleEntityDamage(entityTarget, vPhaseReport, bldg, hits,
                    nCluster, bldgAbsorbs);
        }
        Report r = new Report(3390);
        r.subject = subjectId;
        vPhaseReport.addElement(r);
        if (entityTarget.infernos.isStillBurning()
                || ((target instanceof Tank) && ((Tank) target).isOnFire() && ((Tank) target)
                        .isInfernoFire())) {
            r = new Report(3545);
            r.subject = subjectId;
            r.addDesc(entityTarget);
            r.indent(3);
            int roll = Compute.d6(2);
            r.add(roll);
            if (roll == 12) {
                r.choose(true);
                entityTarget.infernos.clear();
            } else {
                r.choose(false);
            }
            vPhaseReport.add(r);
        } else if ((target instanceof Tank) && ((Tank) target).isOnFire()) {
            r = new Report(3550);
            r.subject = subjectId;
            r.addDesc(entityTarget);
            r.indent(3);
            int roll = Compute.d6(2);
            r.add(roll);
            if (roll >= 4) {
                r.choose(true);
                for (int i = 0; i < entityTarget.locations(); i++) {
                    ((Tank) target).extinguishAll();
                }
            } else {
                r.choose(false);
            }
            vPhaseReport.add(r);
        }
        // coolant also reduces heat of mechs
        if (target instanceof Mech) {
            int nDamage = 3;
            r = new Report(3400);
            r.subject = subjectId;
            r.indent(2);
            r.add(nDamage);
            r.choose(false);
            vPhaseReport.add(r);
            entityTarget.coolFromExternal += nDamage;
            hits = 0;
        }
    }
}

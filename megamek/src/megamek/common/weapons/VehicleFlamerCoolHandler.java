/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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

import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.gameManager.GameManager;

/**
 * @author Sebastian Brocks
 * Created on Sep 23, 2004
 */
public class VehicleFlamerCoolHandler extends AmmoWeaponHandler {
    private static final long serialVersionUID = 4856089237895318515L;

    /**
     * @param toHit
     * @param waa
     * @param g
     */
    public VehicleFlamerCoolHandler(ToHitData toHit, WeaponAttackAction waa, Game g, GameManager m) {
        super(toHit, waa, g, m);
    }

    @Override
    protected void handleEntityDamage(Entity entityTarget, Vector<Report> vPhaseReport,
                                      Building bldg, int hits, int nCluster,
            int bldgAbsorbs) {
        if (entityTarget.isConventionalInfantry()) {
            // 1 point direct-fire ballistic
            nDamPerHit = Compute.directBlowInfantryDamage(1,
                    bDirect ? toHit.getMoS() / 3 : 0,
                    WeaponType.WEAPON_DIRECT_FIRE,
                    ((Infantry) target).isMechanized(),
                    toHit.getThruBldg() != null);
            super.handleEntityDamage(entityTarget, vPhaseReport, bldg, hits, nCluster, bldgAbsorbs);
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
            Roll diceRoll = Compute.rollD6(2);
            r.add(diceRoll);

            if (diceRoll.getIntValue() == 12) {
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
            Roll diceRoll = Compute.rollD6(2);
            r.add(diceRoll);

            if (diceRoll.getIntValue() >= 4) {
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
            int nDamage = (nDamPerHit * hits) + 1;
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

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
package megamek.common.weapons.battlearmor;

import java.util.Vector;

import megamek.common.Compute;
import megamek.common.Game;
import megamek.common.Infantry;
import megamek.common.Report;
import megamek.common.TargetRoll;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.WeaponHandler;
import megamek.server.Server;
import megamek.server.DamageType;

/**
 * @author Sebastian Brockxs
 * @since Oct 20, 2004
 */
public class BAMGHandler extends WeaponHandler {
    private static final long serialVersionUID = 4109377609879352900L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public BAMGHandler(ToHitData t, WeaponAttackAction w, Game g, Server s) {
        super(t, w, g, s);
        damageType = DamageType.ANTI_INFANTRY;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        if (weapon.isRapidfire() && !(target instanceof Infantry)) {
            // Check for rapid fire Option. Only MGs can be rapidfire.
            switch (wtype.getDamage()) {
                case 1:
                    nDamPerHit = Math.max(1, Compute.d6() - 1);
                    break;
                case 3:
                    nDamPerHit = Compute.d6() + 1;
                    break;
                default:
                    nDamPerHit = Compute.d6();
                    break;
            }
            numRapidFireHits = nDamPerHit;
            if (bDirect) {
                nDamPerHit = Math.min(nDamPerHit + (toHit.getMoS() / 3),
                        nDamPerHit * 2);
            }
            nDamPerHit = applyGlancingBlowModifier(nDamPerHit, false);
        } else {
            nDamPerHit = super.calcDamagePerHit();
        }
        return nDamPerHit;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#addHeat()
     */
    @Override
    protected void addHeat() {
        if (!(toHit.getValue() == TargetRoll.IMPOSSIBLE)) {
            if (weapon.isRapidfire()) {
                ae.heatBuildup += nDamPerHit;
            } else {
                super.addHeat();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#reportMiss(java.util.Vector)
     */
    @Override
    protected void reportMiss(Vector<Report> vPhaseReport) {
        // Report the miss
        Report r = new Report(3220);
        r.subject = subjectId;
        vPhaseReport.add(r);
        if (weapon.isRapidfire() && !target.isConventionalInfantry()) {
            r.newlines = 0;
            r = new Report(3225);
            r.subject = subjectId;
            r.add(nDamPerHit * 3);
            vPhaseReport.add(r);
        }       
    }
}

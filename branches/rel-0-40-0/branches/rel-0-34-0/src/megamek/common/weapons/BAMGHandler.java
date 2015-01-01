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
 * Created on Oct 20, 2004
 *
 */
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Report;
import megamek.common.TargetRoll;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;
import megamek.server.Server.DamageType;

/**
 * @author Sebastian Brockxs
 */
public class BAMGHandler extends WeaponHandler {

    /**
     *
     */
    private static final long serialVersionUID = 4109377609879352900L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public BAMGHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
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
            nDamPerHit = Compute.d6();
        } else {
            if ((target instanceof Infantry) && !(target instanceof BattleArmor)) {
                switch (wtype.getDamage()) {
                    case 1:
                        nDamPerHit = (int) Math.ceil(Compute.d6() / 2);
                        break;
                    case 2:
                        nDamPerHit = Compute.d6();
                        break;
                    case 3:
                        nDamPerHit = Compute.d6(2);
                        break;
                }
                if ( bDirect ) {
                    nDamPerHit += toHit.getMoS()/3;
                }
            } else {
                nDamPerHit = super.calcDamagePerHit();
            }
        }
        if (bGlancing) {
            nDamPerHit =(int) Math.floor(nDamPerHit / 2.0);
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
                ae.heatBuildup += (wtype.getHeat());
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
        r = new Report(3220);
        r.subject = subjectId;
        if (weapon.isRapidfire()
                && !((target instanceof Infantry) && !(target instanceof BattleArmor))) {
            r.messageId = 3225;
            r.add(nDamPerHit * 3);
        }
        vPhaseReport.add(r);
    }
}

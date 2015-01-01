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

import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.TargetRoll;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;
import megamek.server.Server.DamageType;

/**
 * @author Andrew Hunter
 */
public class MGHandler extends AmmoWeaponHandler {

    /**
     * 
     */
    private static final long serialVersionUID = 5635871269404561702L;
    
    private int nRapidDamHeatPerHit;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public MGHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
        damageType = DamageType.ANTI_INFANTRY;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    protected int calcDamagePerHit() {
        if (weapon.isRapidfire() && !(target instanceof Infantry && !(target instanceof BattleArmor))) {
            // Check for rapid fire Option. Only MGs can be rapidfire.
            // nDamPerHit was already set in useAmmo
            if (bGlancing)
                nDamPerHit = (int) Math.floor(nDamPerHit / 2.0);
        } else {
            if (target instanceof Infantry && !(target instanceof BattleArmor)) {
                nDamPerHit = Compute.d6(wtype.getDamage());
                if (bGlancing)
                    nDamPerHit = (int) Math.floor(nDamPerHit / 2.0);
                if (bDirect)
                    nDamPerHit += toHit.getMoS()/3;
            } else {
                nDamPerHit = super.calcDamagePerHit();
            }
        }
        if (game.getOptions().booleanOption("tacops_range") && nRange > wtype.getRanges(weapon)[RangeType.RANGE_LONG]) {
            float toReturn = nDamPerHit;
            toReturn *= .75;
            nDamPerHit = (int) Math.floor(toReturn);
        }

        return nDamPerHit;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#addHeat()
     */
    protected void addHeat() {
        if (!(toHit.getValue() == TargetRoll.IMPOSSIBLE)) {
            if (weapon.isRapidfire()) {
                ae.heatBuildup += nRapidDamHeatPerHit;
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
    protected void reportMiss(Vector<Report> vPhaseReport) {
        // Report the miss
        r = new Report(3220);
        r.subject = subjectId;
        if (weapon.isRapidfire()) {
            r.messageId = 3225;
            r.add(nDamPerHit * 3);
        }
        vPhaseReport.add(r);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#useAmmo()
     */
    protected void useAmmo() {
        if (weapon.isRapidfire()) {

            //TacOps p.102 Rapid Fire MG Rules
            switch (wtype.getAmmoType()) {
            case AmmoType.T_MG:
                nDamPerHit = Compute.d6();
                break;
            case AmmoType.T_MG_HEAVY:
                nDamPerHit = Compute.d6() + 1;
                break;
            case AmmoType.T_MG_LIGHT:
                nDamPerHit = Math.max(1, Compute.d6() - 1);
                break;
            }

            nRapidDamHeatPerHit = nDamPerHit;
            checkAmmo();
            int ammoUsage = 3 * nRapidDamHeatPerHit;
            for (int i = 0; i < ammoUsage; i++) {
                if (ammo.getShotsLeft() <= 0) {
                    ae.loadWeapon(weapon);
                    ammo = weapon.getLinked();
                }
                ammo.setShotsLeft(ammo.getShotsLeft() - 1);
            }
            setDone();
        } else
            super.useAmmo();
    }

}

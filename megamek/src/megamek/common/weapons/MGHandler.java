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
import megamek.common.options.OptionsConstants;
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
    @Override
    protected int calcDamagePerHit() {
        double toReturn = nDamPerHit;
        if (weapon.isRapidfire()
            && !((target instanceof Infantry) && !(target instanceof BattleArmor))) {
            // Check for rapid fire Option. Only MGs can be rapidfire.
            // nDamPerHit was already set in useAmmo
            if (bGlancing) {
                toReturn = (int) Math.floor(nDamPerHit / 2.0);
            }
            if (bDirect) {
                toReturn = Math.min(toReturn + (toHit.getMoS() / 3),
                                    toReturn * 2);
            }
        } else {
            if ((target instanceof Infantry)
                    && !(target instanceof BattleArmor)) {
                toReturn = Compute.directBlowInfantryDamage(
                        wtype.getDamage(), bDirect ? toHit.getMoS() / 3 : 0,
                        wtype.getInfantryDamageClass(),
                        ((Infantry) target).isMechanized(),
                        toHit.getThruBldg() != null, ae.getId(), calcDmgPerHitReport);
                if (bGlancing) {
                    toReturn = (int) Math.floor(toReturn / 2.0);
                }
            } else {
                toReturn = wtype.getDamage();
                if (bDirect) {
                    toReturn = Math.min(toReturn + (toHit.getMoS() / 3),
                                        toReturn * 2);
                }
                if (bGlancing) {
                    toReturn = (int) Math.floor(toReturn / 2.0);
                }
            }
        }
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE)
            && (nRange > wtype.getRanges(weapon)[RangeType.RANGE_LONG])) {
            toReturn *= .75;
            toReturn = (int) Math.floor(toReturn);
        }
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_LOS_RANGE)
                && (nRange > wtype.getRanges(weapon)[RangeType.RANGE_EXTREME])) {
            toReturn = (int) Math.floor(toReturn * .5);
        }
        nDamPerHit = (int) toReturn;

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
                ae.heatBuildup += nRapidDamHeatPerHit;
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
        if (weapon.isRapidfire()
            && !((target instanceof Infantry) && !(target instanceof BattleArmor))) {
            r.newlines = 0;
            r = new Report(3225);
            r.subject = subjectId;
            r.add(nDamPerHit * 3);
            vPhaseReport.add(r);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#useAmmo()
     */
    @Override
    protected void useAmmo() {
        if (weapon.isRapidfire()) {

            // TacOps p.102 Rapid Fire MG Rules
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

            numRapidFireHits = nDamPerHit;
            nRapidDamHeatPerHit = nDamPerHit;
            checkAmmo();
            int ammoUsage = 3 * nRapidDamHeatPerHit;
            for (int i = 0; i < ammoUsage; i++) {
                if (ammo.getUsableShotsLeft() <= 0) {
                    ae.loadWeapon(weapon);
                    ammo = weapon.getLinked();
                }
                ammo.setShotsLeft(ammo.getBaseShotsLeft() - 1);
            }
            setDone();
        } else {
            super.useAmmo();
        }
    }

}

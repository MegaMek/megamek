/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
import megamek.server.GameManager;

/**
 * @author Sebastian Brocks
 */
public class NarcExplosiveHandler extends MissileWeaponHandler {
    private static final long serialVersionUID = -1655014339855184419L;

    /**
     * @param t
     * @param w
     * @param g
     * @param m
     */
    public NarcExplosiveHandler(ToHitData t, WeaponAttackAction w, Game g,
            GameManager m) {
        super(t, w, g, m);
        sSalvoType = " explosive pod ";
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcHits(java.util.Vector)
     */
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        getAMSHitsMod(vPhaseReport);
        // conventional infantry gets hit in one lump
        // BAs do one lump of damage per BA suit
        if (target.isConventionalInfantry()) {
            if (ae instanceof BattleArmor) {
                bSalvo = true;
                return ((BattleArmor) ae).getShootingStrength();
            }
            return 1;
        }
        bSalvo = true;
        if (ae instanceof BattleArmor) {
            if (amsEngaged) {
                return Compute.missilesHit(
                        ((BattleArmor) ae).getShootingStrength(), -2);
            }
            return Compute
                    .missilesHit(((BattleArmor) ae).getShootingStrength());
        }

        if (amsEngaged) {
            Report r = new Report(3235);
            r.subject = subjectId;
            vPhaseReport.add(r);
            r = new Report(3230);
            r.indent(1);
            r.subject = subjectId;
            vPhaseReport.add(r);
            Roll diceRoll = Compute.rollD6(1);

            if (diceRoll.getIntValue() <= 3) {
                r = new Report(3240);
                r.subject = subjectId;
                r.add("pod");
                r.add(diceRoll);
                vPhaseReport.add(r);
                return 0;
            }
            r = new Report(3241);
            r.add("pod");
            r.add(diceRoll);
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
    @Override
    protected int calcnCluster() {
        return 1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        AmmoType atype = (AmmoType) ammo.getType();
        double toReturn;
        if (atype.getAmmoType() == AmmoType.T_INARC) {
            toReturn = 6;
        } else {
            toReturn = 4;
        }
        if (target.isConventionalInfantry()) {
            toReturn = Compute.directBlowInfantryDamage(toReturn,
                    bDirect ? toHit.getMoS() / 3 : 0,
                    WeaponType.WEAPON_DIRECT_FIRE,
                    ((Infantry) target).isMechanized(),
                    toHit.getThruBldg() != null, ae.getId(), calcDmgPerHitReport);
            toReturn = Math.ceil(toReturn);
        }
        
        toReturn = applyGlancingBlowModifier(toReturn, target.isConventionalInfantry());
        return (int) toReturn;
    }
}

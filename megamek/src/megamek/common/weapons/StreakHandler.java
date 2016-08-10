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

import megamek.common.BattleArmor;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.ComputeECM;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Report;
import megamek.common.TargetRoll;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public class StreakHandler extends MissileWeaponHandler {

    /**
     *
     */
    private static final long serialVersionUID = 4122111574368642492L;
    boolean isAngelECMAffected = ComputeECM.isAffectedByAngelECM(ae,
            ae.getPosition(), target.getPosition());

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public StreakHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        if ((target instanceof Infantry) && !(target instanceof BattleArmor)) {
            int toReturn = Compute.directBlowInfantryDamage(
                    wtype.getRackSize() * 2, bDirect ? toHit.getMoS() / 3 : 0,
                    wtype.getInfantryDamageClass(),
                    ((Infantry) target).isMechanized(),
                    toHit.getThruBldg() != null, ae.getId(), calcDmgPerHitReport);
            return toReturn;
        }
        return 2;
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
     * @see megamek.common.weapons.WeaponHandler#calcHits(java.util.Vector)
     */
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        // conventional infantry gets hit in one lump
        // BAs do one lump of damage per BA suit
        if ((target instanceof Infantry) && !(target instanceof BattleArmor)) {
            if (ae instanceof BattleArmor) {
                bSalvo = true;
                return ((BattleArmor) ae).getShootingStrength();
            }
            return 1;
        }
        // no AMS when streak misses
        if (bMissed) {
            return 0;
        }
        int nMissilesModifier = getClusterModifiers(true);

        int missilesHit;
        int amsMod = getAMSHitsMod(vPhaseReport);
        if (amsMod == 0 && allShotsHit()) {
            missilesHit = wtype.getRackSize();
        } else {
            missilesHit = Compute.missilesHit(wtype.getRackSize(), amsMod+nMissilesModifier,
                    weapon.isHotLoaded(), allShotsHit(), isAdvancedAMS());
            if (amsMod != 0) {
                Report r;
                if (amsMod > 0) {
                    r = new Report(3340);
                } else {
                    r = new Report(3341);
                }
                r.subject = subjectId;
                r.add(amsMod);
                r.newlines = 0;
                vPhaseReport.addElement(r);
            }
        }
        if (missilesHit > 0) {
            Report r = new Report(3325);
            r.subject = subjectId;
            r.add(missilesHit);
            r.add(sSalvoType);
            r.add(toHit.getTableDesc());
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }
        Report r = new Report(3345);
        r.subject = subjectId;
        vPhaseReport.addElement(r);
        bSalvo = true;
        return missilesHit;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#UseAmmo()
     */
    @Override
    protected void useAmmo() {
        checkAmmo();
        if (ammo == null) {// Can't happen. w/o legal ammo, the weapon
            // *shouldn't* fire.
            System.out.println("Handler can't find any ammo!  Oh no!");
        }
        if (ammo.getUsableShotsLeft() <= 0) {
            ae.loadWeaponWithSameAmmo(weapon);
            ammo = weapon.getLinked();
        }
        if (roll >= toHit.getValue()) {
            ammo.setShotsLeft(ammo.getBaseShotsLeft() - 1);
            if (wtype.hasFlag(WeaponType.F_ONESHOT)) {
                weapon.setFired(true);
            }
            setDone();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#reportMiss(java.util.Vector)
     */
    @Override
    protected void reportMiss(Vector<Report> vPhaseReport) {
        // if (!isAngelECMAffected) {
        // no lock
        Report r = new Report(3215);
        r.subject = subjectId;
        vPhaseReport.addElement(r);
        /*
         * } else { super.reportMiss(vPhaseReport); }
         */
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#addHeat()
     */
    @Override
    protected void addHeat() {
        if (!(toHit.getValue() == TargetRoll.IMPOSSIBLE)
                && (roll >= toHit.getValue())) {
            super.addHeat();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#allShotsHit()
     */
    @Override
    protected boolean allShotsHit() {
        return super.allShotsHit() || !isAngelECMAffected;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.weapons.MissileWeaponHandler#handleSpecialMiss(megamek
     * .common.Entity, boolean, megamek.common.Building, java.util.Vector)
     */
    @Override
    protected boolean handleSpecialMiss(Entity entityTarget,
            boolean bldgDamagedOnMiss, Building bldg,
            Vector<Report> vPhaseReport) {
        return false;
    }

    @Override
    protected boolean canDoDirectBlowDamage() {
        return false;
    }
}

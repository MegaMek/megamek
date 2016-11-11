/**
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 29, 2004
 *
 */
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 */
public class UltraWeaponHandler extends AmmoWeaponHandler {
    /**
     *
     */
    private static final long serialVersionUID = 7551194199079004134L;
    int howManyShots;
    private final boolean twoRollsUltra; // Tracks whether or not this is an

    // ultra AC using the unofficial "two rolls" rule. Can be final because
    // this isn't really going to change over the course of a game.

    /**
     * @param t
     * @param w
     * @param g
     */
    public UltraWeaponHandler(ToHitData t, WeaponAttackAction w, IGame g,
            Server s) {
        super(t, w, g, s);
        twoRollsUltra = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_UAC_TWOROLLS)
                && ((wtype.getAmmoType() == AmmoType.T_AC_ULTRA) || (wtype
                        .getAmmoType() == AmmoType.T_AC_ULTRA_THB));
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#addHeatUseAmmo()
     */
    @Override
    protected void useAmmo() {
        setDone();
        checkAmmo();
        int total = ae.getTotalAmmoOfType(ammo.getType());
        if ((total > 1) && !weapon.curMode().equals("Single")) {
            howManyShots = 2;
        } else {
            howManyShots = 1;
        }
        if (total == 0) {
            // can't happen?

        }
        if (ammo.getUsableShotsLeft() == 0) {
            ae.loadWeapon(weapon);
            ammo = weapon.getLinked();
            // there will be some ammo somewhere, otherwise shot will not have
            // been fired.
        }
        if (ammo.getUsableShotsLeft() == 1) {
            ammo.setShotsLeft(0);
            ae.loadWeapon(weapon);
            ammo = weapon.getLinked();
            // that fired one, do we need to fire another?
            ammo.setShotsLeft(ammo.getBaseShotsLeft()
                    - ((howManyShots == 2) ? 1 : 0));
        } else {
            ammo.setShotsLeft(ammo.getBaseShotsLeft() - howManyShots);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcHits(java.util.Vector)
     */
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        // conventional infantry gets hit in one lump
        // BAs can't mount UACS/RACs
        if ((target instanceof Infantry) && !(target instanceof BattleArmor)) {
            return 1;
        }

        bSalvo = true;

        if (howManyShots == 1 || twoRollsUltra) {
            return 1;
        }

        int shotsHit;
        int nMod = getClusterModifiers(true);

        shotsHit = allShotsHit() ? howManyShots : Compute.missilesHit(
                howManyShots, nMod);

        // report number of shots that hit only when weapon doesn't jam
        if (!weapon.isJammed()) {
            Report r = new Report(3325);
            r.subject = subjectId;
            r.add(shotsHit);
            r.add(sSalvoType);
            r.add(toHit.getTableDesc());
            r.newlines = 0;
            vPhaseReport.addElement(r);
            if (nMod != 0) {
                if (nMod > 0) {
                    r = new Report(3340);
                } else {
                    r = new Report(3341);
                }
                r.subject = subjectId;
                r.add(nMod);
                r.newlines = 0;
                vPhaseReport.addElement(r);
            }
            r = new Report(3345);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }
        return shotsHit;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#doChecks(java.util.Vector)
     */
    @Override
    protected boolean doChecks(Vector<Report> vPhaseReport) {
        if ((roll == 2) && (howManyShots == 2) && !(ae instanceof Infantry)) {
            Report r = new Report();
            r.subject = subjectId;
            weapon.setJammed(true);
            isJammed = true;
            if ((wtype.getAmmoType() == AmmoType.T_AC_ULTRA)
                    || (wtype.getAmmoType() == AmmoType.T_AC_ULTRA_THB)) {
                r.messageId = 3160;
            } else {
                r.messageId = 3170;
            }
            vPhaseReport.addElement(r);
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        double toReturn = wtype.getDamage();
        // infantry get hit by all shots
        if ((target instanceof Infantry) && !(target instanceof BattleArmor)) {
            if (howManyShots > 1) { // Is this a cluser attack?
                // Compute maximum damage potential for cluster weapons
                toReturn = howManyShots * wtype.getDamage();
                toReturn = Compute.directBlowInfantryDamage(toReturn,
                        bDirect ? toHit.getMoS() / 3 : 0,
                        WeaponType.WEAPON_CLUSTER_BALLISTIC, // treat as cluster
                        ((Infantry) target).isMechanized(),
                        toHit.getThruBldg() != null, ae.getId(),
                        calcDmgPerHitReport);
            } else { // No - only one shot fired
                toReturn = Compute.directBlowInfantryDamage(wtype.getDamage(),
                        bDirect ? toHit.getMoS() / 3 : 0,
                        wtype.getInfantryDamageClass(),
                        ((Infantry) target).isMechanized(),
                        toHit.getThruBldg() != null, ae.getId(),
                        calcDmgPerHitReport);
            }
        // Cluster bonuses or penalties can't apply to "two rolls" UACs, so
        // if we have one, modify the damage per hit directly.
        } else if (bDirect && (howManyShots == 1 || twoRollsUltra)) {
            toReturn = Math.min(toReturn + (toHit.getMoS() / 3), toReturn * 2);
        }

        if (bGlancing && (howManyShots == 1 || twoRollsUltra)) {
            toReturn = (int) Math.floor(toReturn / 2.0);
        }

        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE)
                && (nRange > wtype.getRanges(weapon)[RangeType.RANGE_LONG])) {
            toReturn = (int) Math.floor(toReturn * .75);
        }
        if (game.getOptions().booleanOption(
                OptionsConstants.ADVCOMBAT_TACOPS_LOS_RANGE)
                && (nRange > wtype.getRanges(weapon)[RangeType.RANGE_EXTREME])) {
            toReturn = (int) Math.floor(toReturn * .5);
        }
        return (int) toReturn;
    }

    @Override
    protected boolean usesClusterTable() {
        return !game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_UAC_TWOROLLS);
    }

    @Override
    protected int calcnClusterAero(Entity entityTarget) {
        if (usesClusterTable() && !ae.isCapitalFighter()
                && (entityTarget != null) && !entityTarget.isCapitalScale()) {
            return (int) Math.ceil(attackValue / 2.0);
        } else {
            return 1;
        }
    }

}

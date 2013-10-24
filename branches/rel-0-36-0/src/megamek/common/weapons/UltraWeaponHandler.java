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
import megamek.common.actions.WeaponAttackAction;
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

    /**
     * @param t
     * @param w
     * @param g
     */
    public UltraWeaponHandler(ToHitData t, WeaponAttackAction w, IGame g,
            Server s) {
        super(t, w, g, s);
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

        if ((howManyShots == 1)
                || (game.getOptions().booleanOption("uac_tworolls") && ((wtype
                        .getAmmoType() == AmmoType.T_AC_ULTRA) || (wtype
                        .getAmmoType() == AmmoType.T_AC_ULTRA_THB)))) {
            return 1;
        }

        int shotsHit;
        int nMod = 0;
        if (bGlancing) {
            nMod -= 4;
        }
        if (game.getPlanetaryConditions().hasEMI()) {
            nMod -= 2;
        }

        if (bDirect) {
            nMod += (toHit.getMoS() / 3) * 2;
        }

        boolean tacopscluster = game.getOptions().booleanOption(
                "tacops_clusterhitpen");

        int[] ranges = wtype.getRanges(weapon);
        if (tacopscluster) {
            if (nRange <= 1) {
                nMod += 1;
            } else if (nRange <= ranges[RangeType.RANGE_MEDIUM]) {
                nMod += 0;
            } else {
                nMod -= 1;
            }
        }

        if (game.getOptions().booleanOption("tacops_range")
                && (nRange > ranges[RangeType.RANGE_LONG])) {
            nMod -= 2;
        }

        shotsHit = allShotsHit() ? howManyShots : Compute.missilesHit(
                howManyShots, nMod);

        // report number of shots that hit only when weapon doesn't jam
        if (!weapon.isJammed()) {
            Report r = new Report(3325);
            r.subject = subjectId;
            r.add(shotsHit);
            r.add(" shot(s) ");
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
                if (!game.getOptions().booleanOption("uac_tworolls")) {
                    weapon.setHit(true);
                }
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
            toReturn = 0;
            for (int i = 0; i < howManyShots; i++) {
                toReturn += Compute.directBlowInfantryDamage(wtype.getDamage(),
                        bDirect ? toHit.getMoS() / 3 : 0,
                        wtype.getInfantryDamageClass(),
                        ((Infantry) target).isMechanized());
            }
            // plus 1 for cluster
            toReturn++;
        } else if (bDirect) {
            toReturn = Math.min(toReturn + (toHit.getMoS() / 3), toReturn * 2);
        }

        if (bGlancing) {
            toReturn = (int) Math.floor(toReturn / 2.0);
        }

        if (game.getOptions().booleanOption("tacops_range")
                && (nRange > wtype.getRanges(weapon)[RangeType.RANGE_LONG])) {
            toReturn = (int) Math.floor(toReturn * .75);
        }
        return (int) toReturn;
    }

    @Override
    protected boolean usesClusterTable() {
        return !game.getOptions().booleanOption("uac_tworolls");
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

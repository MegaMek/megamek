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
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 * 
 */
public class UltraWeaponHandler extends AmmoWeaponHandler {
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
    protected void useAmmo() {
        setDone();
        checkAmmo();
        int total = ae.getTotalAmmoOfType(ammo.getType());
        if (total > 1) {
            howManyShots = 2;
        }
        if (total == 1) {
            howManyShots = 1;
        }
        if (total == 0) {
            // can't happen?

        }
        if (ammo.getShotsLeft() == 0) {
            ae.loadWeapon(weapon);
            ammo = weapon.getLinked();
            // there will be some ammo somewhere, otherwise shot will not have
            // been fired.
        }
        if (ammo.getShotsLeft() == 1) {
            ammo.setShotsLeft(0);
            ae.loadWeapon(weapon);
            ammo = weapon.getLinked();
            // that fired one, do we need to fire another?
            ammo.setShotsLeft(ammo.getShotsLeft()
                    - ((howManyShots == 2) ? 1 : 0));
        } else {
            ammo.setShotsLeft(ammo.getShotsLeft() - howManyShots);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcHits(java.util.Vector)
     */
    protected int calcHits(Vector<Report> vPhaseReport) {
        // conventional infantry gets hit in one lump
        // BAs do one lump of damage per BA suit
        if (target instanceof Infantry && !(target instanceof BattleArmor)) {
            if (ae instanceof BattleArmor) {
                bSalvo = true;
                return ((BattleArmor)ae).getShootingStrength();
            }
            return 1;
        }
        int shotsHit;
        switch (howManyShots) {
            case 1:
                shotsHit = 1;
                break;
            default:
                shotsHit = allShotsHit() ? howManyShots : Compute
                        .missilesHit(howManyShots);
                // report number of shots that hit only when weapon doesn't jam
                if (!weapon.isJammed()) {
                    r = new Report(3325);
                    r.subject = subjectId;
                    r.add(shotsHit);
                    r.add(" shot(s) ");
                    r.add(toHit.getTableDesc());
                    r.newlines = 0;
                    vPhaseReport.addElement(r);
                    r = new Report(3345);
                    r.newlines = 0;
                    vPhaseReport.addElement(r);
                }
                break;
        }
        bSalvo = true;
        return shotsHit;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#addHeat()
     */
    protected void addHeat() {// silly hack
        for (int x = 0; x < howManyShots; x++) {
            super.addHeat();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#doChecks()
     */
    protected boolean doChecks(Vector<Report> vPhaseReport) {
        if (roll == 2 && howManyShots == 2) {
            r = new Report(); 
            r.subject = subjectId;
            weapon.setJammed(true);
            if (wtype.getAmmoType() == AmmoType.T_AC_ULTRA) {
                r.messageId = 3160;
                weapon.setHit(true);
            } else {
                r.messageId = 3170;
            }
            vPhaseReport.addElement(r);
            return true;
        }
        return false;
    }
}

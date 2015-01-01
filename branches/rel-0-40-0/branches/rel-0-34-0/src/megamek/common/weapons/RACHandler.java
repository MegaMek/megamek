/**
 * MegaMek - Copyright (C) 2004,2005,2006,2007 Ben Mazur (bmazur@sev.org)
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
 * Created on Oct 19, 2004
 *
 */
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.IGame;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 */
public class RACHandler extends UltraWeaponHandler {
    /**
     * 
     */
    private static final long serialVersionUID = -4859480151505343638L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public RACHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.UltraWeaponHandler#doChecks(java.util.Vector)
     */
    protected boolean doChecks(Vector<Report> vPhaseReport) {
        boolean jams = false;
        switch (howManyShots) {
            case 6:
                if (roll <= 4) {
                    jams = true;
                }
                break;
            case 5:
            case 4:
                if (roll <= 3) {
                    jams = true;
                }
                break;
            case 3:
            case 2:
                if (roll <= 2) {
                    jams = true;
                }
                break;
            default:
                break;
        }
        if (jams) {
            r = new Report(3160);
            r.subject = subjectId;
            r.add(" shot(s)");
            vPhaseReport.addElement(r);
            weapon.setJammed(true);
            return false;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#useAmmo()
     */
    protected void useAmmo() {
        int actualShots;
        setDone();
        checkAmmo();
        if (weapon.curMode().equals("6-shot")) {
            howManyShots = 6;
        } else if (weapon.curMode().equals("5-shot")) {
            howManyShots = 5;
        }    else if (weapon.curMode().equals("4-shot")) {
            howManyShots = 4;
        } else if (weapon.curMode().equals("3-shot")) {
            howManyShots = 3;
        } else if (weapon.curMode().equals("2-shot")) {
            howManyShots = 2;
        } else if (weapon.curMode().equals("Single")) {
            howManyShots = 1;
        }
        int total = ae.getTotalAmmoOfType(ammo.getType());
        if (total >= 6) {
            actualShots = 6;
        } else if (total >= 5) {
            actualShots = 5;
        } else if (total >= 3) {
            actualShots = 3;
        } else if (total >= 2) {
            actualShots = 2;
        } else {
            actualShots = 1;
        }
        if (actualShots < howManyShots)
            howManyShots = actualShots;
        int shotsNeedFiring = howManyShots;
        if (ammo.getShotsLeft() == 0) {
            ae.loadWeapon(weapon);
            ammo = weapon.getLinked();
            // there will be some ammo somewhere, otherwise shot will not have
            // been fired.
        }

        while (shotsNeedFiring > ammo.getShotsLeft()) {
            shotsNeedFiring -= ammo.getShotsLeft();
            ammo.setShotsLeft(0);
            ae.loadWeapon(weapon);
            ammo = weapon.getLinked();
        }
        ammo.setShotsLeft(ammo.getShotsLeft() - shotsNeedFiring);
    }
    
    protected boolean usesClusterTable() {
        return true;
    }
    
    protected boolean canDoDirectBlowDamage(){
        return false;
    }

}

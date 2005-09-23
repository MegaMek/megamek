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
 * 
 */
public class RAC4Handler extends UltraWeaponHandler {
    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public RAC4Handler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#doChecks()
     */
    protected boolean doChecks(Vector vPhaseReport) {
        boolean jams = false;
        switch (howManyShots) {
            case 4:
                if (roll <= 3) {
                    jams = true;
                }
                break;
            case 2:
                if (roll <= 2) {
                    jams = true;
                }
                break;
        }
        if (jams) {
            r = new Report(3160);
            r.subject = subjectId;
            r.add(" shot(s)");
            vPhaseReport.addElement(r);
            weapon.setJammed(true);
            return true;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#useAmmo()
     */
    protected void useAmmo() {
        int shotsNeedFiring;
        setDone();
        checkAmmo();
        int total = ae.getTotalAmmoOfType(ammo.getType());
        if (total >= 4) {
            howManyShots = 4;
        } else if (total >= 2) {
            howManyShots = 2;
        } else {
            howManyShots = 1;
        }
        shotsNeedFiring = howManyShots;
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
}

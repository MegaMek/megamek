/*
 * MegaMek - Copyright (C) 2004, 2005, 2006, 2007 Ben Mazur (bmazur@sev.org)
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

import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.Infantry;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.GameManager;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 * @since Oct 19, 2004
 */
public class RACHandler extends UltraWeaponHandler {
    private static final long serialVersionUID = -4859480151505343638L;

    /**
     * @param t
     * @param w
     * @param g
     * @param m
     */
    public RACHandler(ToHitData t, WeaponAttackAction w, Game g, GameManager m) {
        super(t, w, g, m);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.UltraWeaponHandler#doChecks(java.util.Vector)
     */
    @Override
    protected boolean doChecks(Vector<Report> vPhaseReport) {
        if (doAmmoFeedProblemCheck(vPhaseReport)) {
            return true;
        }

        if (ae instanceof Infantry) {
            return false;
        }
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
            Report r = new Report(3160);
            r.subject = subjectId;
            r.add(" shot(s)");
            vPhaseReport.addElement(r);
            weapon.setJammed(true);
        }

        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#useAmmo()
     */
    @Override
    protected void useAmmo() {
        int actualShots;
        setDone();
        checkAmmo();

        switch (weapon.curMode().toString()){
            case Weapon.MODE_RAC_SIX_SHOT: howManyShots = 6;
                break;
            case Weapon.MODE_RAC_FIVE_SHOT: howManyShots = 5;
                break;
            case Weapon.MODE_RAC_FOUR_SHOT: howManyShots = 4;
                break;
            case Weapon.MODE_RAC_THREE_SHOT: howManyShots = 3;
                break;
            case Weapon.MODE_RAC_TWO_SHOT: howManyShots = 2;
                break;
            case Weapon.MODE_AC_SINGLE: howManyShots = 1;
                break;
        }

        // Reduce number of allowed shots to number of remaining rounds of ammo if applicable
        int total = ae.getTotalAmmoOfType(ammo.getType());
        if (total < 0 ) {
            throw new RuntimeException("Invalid total ammo value < 0!");
        } else if (total < 6) {
            actualShots = total;
        } else {
            actualShots = 6;
        }
        if (actualShots < howManyShots) {
            howManyShots = actualShots;
        }

        int shotsNeedFiring = howManyShots;

        // Try to reload if the linked bin is empty but another exists
        attemptToReloadWeapon();

        // Reduce linked ammo bin; if it runs out, switch to another.
        reduceShotsLeft(shotsNeedFiring);
    }

    @Override
    protected boolean usesClusterTable() {
        return true;
    }

    @Override
    protected int calcnClusterAero(Entity entityTarget) {
        return 5;
    }

}

/*
 * Copyright (C) 2004, 2005, 2006, 2007 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2007-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.weapons.handlers;

import java.io.Serial;
import java.util.Vector;

import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.compute.Compute;
import megamek.common.equipment.AmmoType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.Roll;
import megamek.common.units.Entity;
import megamek.common.weapons.Weapon;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Andrew Hunter
 * @since Oct 19, 2004
 */
public class RACHandler extends UltraWeaponHandler {
    @Serial
    private static final long serialVersionUID = -4859480151505343638L;

    /**
     *
     */
    public RACHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) throws EntityLoadingException {
        super(t, w, g, m);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.UltraWeaponHandler#doChecks(java.util.Vector)
     */
    @Override
    protected boolean doChecks(Vector<Report> vPhaseReport) {
        if (doAmmoFeedProblemCheck(vPhaseReport)) {
            return true;
        } else if (attackingEntity.isConventionalInfantry()) {
            return false;
        }
        boolean jams = false;
        switch (howManyShots) {
            case 6:
                if (roll.getIntValue() <= 4) {
                    jams = true;
                }
                break;
            case 5:
            case 4:
                if (roll.getIntValue() <= 3) {
                    jams = true;
                }
                break;
            case 3:
            case 2:
                if (roll.getIntValue() <= 2) {
                    jams = true;
                }
                break;
            default:
                break;
        }

        // PLAYTEST3 Caseless ammo support for RAC
        // Will potentially explode when rolling a 2. Can still jam if not blowing up.
        if (game.getOptions().booleanOption(OptionsConstants.PLAYTEST_3)) {
            if ((roll.getIntValue() <= 2) && !attackingEntity.isConventionalInfantry()
                && ammoType.getMunitionType().contains(AmmoType.Munitions.M_CASELESS)) {
                Roll diceRoll = Compute.rollD6(2);

                Report r = new Report(3164);
                r.subject = subjectId;
                r.add(diceRoll);

                if (diceRoll.getIntValue() >= 8) {
                    // Round explodes destroying weapon
                    weapon.setDestroyed(true);
                    r.choose(false);
                } else {
                    // Just a jam
                    jams = true;
                    r.choose(true);
                }
                vPhaseReport.addElement(r);
            }
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
     * @see megamek.common.weapons.handlers.WeaponHandler#useAmmo()
     */
    @Override
    protected void useAmmo() {
        int actualShots;
        setDone();
        checkAmmo();

        switch (weapon.curMode().toString()) {
            case Weapon.MODE_RAC_SIX_SHOT:
                howManyShots = 6;
                break;
            case Weapon.MODE_RAC_FIVE_SHOT:
                howManyShots = 5;
                break;
            case Weapon.MODE_RAC_FOUR_SHOT:
                howManyShots = 4;
                break;
            case Weapon.MODE_RAC_THREE_SHOT:
                howManyShots = 3;
                break;
            case Weapon.MODE_RAC_TWO_SHOT:
                howManyShots = 2;
                break;
            case Weapon.MODE_AC_SINGLE:
                howManyShots = 1;
                break;
        }

        // Reduce number of allowed shots to number of remaining rounds of ammo if applicable
        int total = attackingEntity.getTotalAmmoOfType(ammo.getType());
        if (total < 0) {
            throw new RuntimeException("Invalid total ammo value < 0!");
        } else {
            actualShots = Math.min(total, 6);
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
    protected int calculateNumClusterAero(Entity entityTarget) {
        return 5;
    }

}

/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.autocannons;

import megamek.common.AmmoType;
import megamek.common.Game;
import megamek.common.SimpleTechLevel;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AmmoWeapon;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.RifleWeaponHandler;
import megamek.server.gameManager.GameManager;

/**
 * @author Jason Tighe
 * @since Sep 25, 2004
 */
public abstract class RifleWeapon extends AmmoWeapon {
    private static final long serialVersionUID = -5777058832149749312L;

    public RifleWeapon() {
        super();

        flags = flags.or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_MECH_WEAPON)
                .or(F_AERO_WEAPON).or(F_TANK_WEAPON);
        ammoType = AmmoType.T_RIFLE;
        explosive = false; // when firing incendiary ammo
        atClass = CLASS_AC;
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_B)
                .setAvailability(RATING_C, RATING_F, RATING_X, RATING_D)
                .setISAdvancement(DATE_PS, DATE_NONE, 3084, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.Game,
     * megamek.server.Server)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
                                              GameManager manager) {
        return new RifleWeaponHandler(toHit, waa, game, manager);
    }
}

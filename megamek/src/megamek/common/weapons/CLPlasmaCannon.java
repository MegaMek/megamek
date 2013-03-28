/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
 * Created on May 29, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.IGame;
import megamek.common.TechConstants;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public class CLPlasmaCannon extends AmmoWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 322396740172378519L;

    public CLPlasmaCannon() {
        techLevel = TechConstants.T_CLAN_TW;
        name = "Plasma Cannon";
        setInternalName("CLPlasmaCannon");
        heat = 7;
        damage = DAMAGE_VARIABLE;
        rackSize = 2;
        ammoType = AmmoType.T_PLASMA;
        minimumRange = WEAPON_NA;
        shortRange = 6;
        mediumRange = 12;
        longRange = 18;
        extremeRange = 24;
        tonnage = 3.0f;
        criticals = 1;
        flags = flags.or(F_MECH_WEAPON).or(F_TANK_WEAPON).or(F_AERO_WEAPON).or(F_PLASMA).or(F_DIRECT_FIRE);
        bv = 170;
        cost = 320000;
        shortAV = 10;
        medAV = 10;
        maxRange = RANGE_MED;
        atClass = CLASS_PLASMA;
        introDate = 3069;
        availRating = new int[]{RATING_X,RATING_X,RATING_D};
        techRating = RATING_F;

    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     *      megamek.common.actions.WeaponAttackAction, megamek.common.Game,
     *      megamek.server.Server)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        return new PlasmaCannonHandler(toHit, waa, game, server);
    }
}

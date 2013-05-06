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
 * Created on Oct 19, 2004
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
 * @author Andrew Hunter
 */
public class ISHGaussRifle extends GaussWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -2379383217525139478L;

    /**
     *
     */
    public ISHGaussRifle() {
        super();
        techLevel.put(3071,TechConstants.T_IS_TW_NON_BOX);
        name = "Heavy Gauss Rifle";
        setInternalName("ISHeavyGaussRifle");
        addLookupName("IS Heavy Gauss Rifle");
        heat = 2;
        damage = DAMAGE_VARIABLE;
        ammoType = AmmoType.T_GAUSS_HEAVY;
        minimumRange = 4;
        shortRange = 6;
        mediumRange = 13;
        longRange = 20;
        extremeRange = 26;
        damageShort = 25;
        damageMedium = 20;
        damageLong = 10;
        tonnage = 18.0f;
        criticals = 11;
        bv = 346;
        cost = 500000;
        shortAV = 25;
        medAV = 20;
        longAV = 10;
        maxRange = RANGE_LONG;
        explosionDamage = 25;
        introDate = 3061;
        techLevel.put(3061,techLevel.get(3071));
        availRating = new int[]{RATING_X,RATING_X,RATING_E};
        techRating = RATING_E;
    }

    @Override
    public int getDamage(int range) {
        if ( range <= shortRange ) {
            return damageShort;
        }

        if ( range <= mediumRange ) {
            return damageMedium;
        }

        return damageLong;
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
        return new HGRHandler(toHit, waa, game, server);
    }
}

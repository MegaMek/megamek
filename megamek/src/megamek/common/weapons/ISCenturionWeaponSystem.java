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
 * Created on Sep 13, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.IGame;
import megamek.common.TechConstants;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public class ISCenturionWeaponSystem extends EnergyWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 5355363156621487309L;

    /**
     *
     */
    public ISCenturionWeaponSystem() {
        super();
        techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        name = "Centurion Weapon System";
        setInternalName(name);
        heat = 4;
        damage = 0;
        minimumRange = 0;
        shortRange = 6;
        mediumRange = 12;
        longRange = 18;
        extremeRange = 24;
        tonnage = 5.0f;
        criticals = 2;
        bv = 190;
        cost = 0;
        shortAV = 0;
        medAV = 0;
        waterShortRange = 4;
        waterMediumRange = 7;
        waterLongRange = 10;
        waterExtremeRange = 14;
        maxRange = RANGE_MED;
        availRating = new int[] { RATING_F, RATING_F, RATING_F };
        techRating = RATING_E;
        flags = flags.or(F_MECH_WEAPON).or(F_TANK_WEAPON).or(F_AERO_WEAPON)
                .or(F_CWS);
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
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        return new CenturionWeaponSystemHandler(toHit, waa, game, server);
    }    

}

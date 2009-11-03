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
 * Created on Sep 7, 2005
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
public class InfantryLaserWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -9065123199493897216L;

    public InfantryLaserWeapon() {
        super();
        techLevel = TechConstants.T_ALLOWED_ALL;
        name = "Infantry Laser";
        setInternalName(name);
        addLookupName("InfantryLaser");
        shortRange = 2;
        mediumRange = 4;
        longRange = 6;
        extremeRange = 8;
        // laser rifle, TM p. 299
        cost = 1250;
        // laser rifle, TM p. 319
        bv = 0.88;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_LASER).or(F_ENERGY);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     *      megamek.common.actions.WeaponAttackAction, megamek.common.IGame)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        return new InfantryLaserHandler(toHit, waa, game, server);
    }
}

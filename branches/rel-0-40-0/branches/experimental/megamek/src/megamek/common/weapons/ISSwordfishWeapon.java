/* MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 25, 2004
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
 * @author Jay Lawson
 */
public class ISSwordfishWeapon extends SubCapitalMissileWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 3827228773281489872L;

    /**
     * 
     */
    public ISSwordfishWeapon() {
        super();
        this.techLevel.put(3071, TechConstants.T_IS_ADVANCED);
        this.name = "Swordfish";
        this.setInternalName(this.name);
        this.addLookupName("Swordfish");
        this.heat = 15;
        this.damage = 4;
        this.ammoType = AmmoType.T_SWORDFISH;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 21;
        this.extremeRange = 28;
        this.tonnage = 140.0f;
        this.bv = 317;
        this.cost = 110000;
        this.shortAV = 4;
        this.maxRange = RANGE_SHORT;
        introDate = 3060;
        techLevel.put(3060,techLevel.get(3071));
        techLevel.put(3071, TechConstants.T_IS_TW_ALL);
        availRating = new int[] { RATING_X, RATING_X, RATING_F };
        techRating = RATING_F;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.IGame)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        return new SwordfishHandler(toHit, waa, game, server);
    }
}

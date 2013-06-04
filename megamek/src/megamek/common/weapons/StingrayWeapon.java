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
public class StingrayWeapon extends SubCapitalMissileWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 3827228773281489872L;

    /**
     * 
     */
    public StingrayWeapon() {
        super();
        this.techLevel.put(3071, TechConstants.T_IS_ADVANCED);
        this.name = "Stingray";
        this.setInternalName(this.name);
        this.addLookupName("Stingray");
        this.heat = 9;
        this.damage = 3;
        this.ammoType = AmmoType.T_STINGRAY;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 21;
        this.extremeRange = 28;
        this.tonnage = 120.0f;
        this.bv = 496;
        this.cost = 85000;
        this.shortAV = 3.5;
        this.medAV = 3.5;
        this.maxRange = RANGE_MED;
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
        return new StingrayHandler(toHit, waa, game, server);
    }
}

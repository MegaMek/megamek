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

import megamek.common.AmmoType;
import megamek.common.IGame;
import megamek.common.TechConstants;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public class InfantryRifleWeapon extends InfantryWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryRifleWeapon() {
        super();
        this.techLevel = TechConstants.T_ALLOWED_ALL;
        this.name = "Infantry Rifle";
        this.setInternalName(this.name);
        this.addLookupName("InfantryRifle");
        this.ammoType = AmmoType.T_AC;
        this.shortRange = 1;
        this.mediumRange = 2;
        this.longRange = 3;
        this.extremeRange = 3;
        // auto-rifle from TM p. 319
        this.bv = 1.28;
        this.flags |= F_DIRECT_FIRE | F_NO_FIRES | F_BALLISTIC;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     *      megamek.common.actions.WeaponAttackAction, megamek.common.IGame)
     */
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        return new InfantryRifleHandler(toHit, waa, game, server);
    }
}

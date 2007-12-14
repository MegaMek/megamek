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
 * 
 */
public class InfantryLRMWeapon extends InfantryWeapon {
    
    public InfantryLRMWeapon() {
        super();
        this.techLevel = TechConstants.T_IS_LEVEL_2;
        this.name = "Infantry LRM";
        this.setInternalName(this.name);
        this.addLookupName("InfantryLRM");
        this.ammoType = AmmoType.T_LRM;
        this.minimumRange = 3;
        this.shortRange = 6;
        this.mediumRange = 9;
        this.longRange = 12;
        this.extremeRange = 18;
        this.bv = 4;
        this.flags |= F_DIRECT_FIRE | F_NO_FIRES | F_MISSILE;
        this.setModes(new String[] {"", "Indirect"});
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     *      megamek.common.actions.WeaponAttackAction, megamek.common.IGame)
     */
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        return new InfantryLRMHandler(toHit, waa, game, server);
    }
}

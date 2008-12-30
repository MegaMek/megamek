/**
 * MegaMek - Copyright (C) 2004,2005,2006,2007 Ben Mazur (bmazur@sev.org)
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
public class ISLightMGA extends AmmoWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -2408433911213524154L;

    public ISLightMGA() {
        super();
        techLevel = TechConstants.T_IS_TW_NON_BOX;
        name = "Light Machine Gun Array";
        addLookupName("IS Light Machine Gun Array");
        setInternalName("ISLMGA");
        heat = 0;
        damage = 1;
        rackSize = 1;
        ammoType = AmmoType.T_MG_LIGHT;
        minimumRange = WEAPON_NA;
        shortRange = 2;
        mediumRange = 4;
        longRange = 6;
        extremeRange = 8;
        tonnage = 0.5f;
        criticals = 1;
        bv = 0; // we'll have to calculate this in calculateBV(),
        // because it depends on the number of MGs linked to
        // the MGA
        flags |= F_BALLISTIC | F_BURST_FIRE | F_MGA;
        cost = 5000;
        String[] modes = { "Linked", "Off" };
        setModes(modes);
        instantModeSwitch = false;
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData, megamek.common.actions.WeaponAttackAction, megamek.common.Game, megamek.server.Server)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, IGame game, Server server) {
        return new MGAWeaponHandler(toHit, waa, game, server);
    }

}

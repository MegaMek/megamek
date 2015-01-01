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
public class CLLightMGA extends AmmoWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 5151562824587975407L;

    public CLLightMGA() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "Light Machine Gun Array";
        this.addLookupName("Clan Light Machine Gun Array");
        this.setInternalName("CLLMGA");
        this.heat = 0;
        this.damage = 1;
        this.rackSize = 1;
        this.ammoType = AmmoType.T_MG_LIGHT;
        this.minimumRange = WEAPON_NA;
        this.shortRange = 2;
        this.mediumRange = 4;
        this.longRange = 6;
        this.extremeRange = 8;
        this.tonnage = 0.25f;
        this.criticals = 1;
        this.bv = 0; // we'll have to calculate this in calculateBV(),
        // because it depends on the number of MGs linked to
        // the MGA
        this.flags |= F_BALLISTIC | F_BURST_FIRE | F_MGA;
        this.cost = 5000;
        String[] modes = { "Linked", "Off" };
        this.setModes(modes);
        this.instantModeSwitch = false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     *      megamek.common.actions.WeaponAttackAction, megamek.common.Game,
     *      megamek.server.Server)
     */
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        return new MGAWeaponHandler(toHit, waa, game, server);
    }

}

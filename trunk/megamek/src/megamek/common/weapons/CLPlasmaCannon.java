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
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "Plasma Cannon";
        this.setInternalName("CLPlasmaCannon");
        this.heat = 7;
        this.damage = DAMAGE_VARIABLE;
        this.rackSize = 2;
        this.ammoType = AmmoType.T_PLASMA;
        this.minimumRange = WEAPON_NA;
        this.shortRange = 6;
        this.mediumRange = 12;
        this.longRange = 18;
        this.extremeRange = 24;
        this.tonnage = 3.0f;
        this.criticals = 1;
        this.flags |= F_PLASMA | F_DIRECT_FIRE;
        this.bv = 170;
        this.cost = 480000;
        this.shortAV = 10;
        this.medAV = 10;
        this.maxRange = RANGE_MED;
        this.atClass = CLASS_PLASMA;
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
        return new PlasmaCannonHandler(toHit, waa, game, server);
    }
}

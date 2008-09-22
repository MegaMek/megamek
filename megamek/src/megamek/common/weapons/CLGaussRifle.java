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
public class CLGaussRifle extends GaussWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = -4436936560457546313L;

    /**
     * 
     */
    public CLGaussRifle() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "Gauss Rifle";
        this.setInternalName("CLGaussRifle");
        this.addLookupName("Clan Gauss Rifle");
        this.heat = 1;
        this.damage = 15;
        this.ammoType = AmmoType.T_GAUSS;
        this.minimumRange = 2;
        this.shortRange = 7;
        this.mediumRange = 15;
        this.longRange = 22;
        this.extremeRange = 30;
        this.tonnage = 12.0f;
        this.criticals = 6;
        this.bv = 320;
        this.cost = 300000;
        this.shortAV = 15;
        this.medAV = 15;
        this.longAV = 15;
        this.maxRange = RANGE_LONG;
        this.explosionDamage = 20;
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
        return new GRHandler(toHit, waa, game, server);
    }

}

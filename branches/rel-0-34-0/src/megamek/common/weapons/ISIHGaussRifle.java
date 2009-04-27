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
 * @author Jason Tighe
 */
public class ISIHGaussRifle extends GaussWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = -2379383217525139478L;

    /**
     * 
     */
    public ISIHGaussRifle() {
        super();
        this.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        this.name = "Improved Heavy Gauss Rifle";
        this.setInternalName("ISImprovedHeavyGaussRifle");
        this.addLookupName("IS Improved Heavy Gauss Rifle");
        this.heat = 2;
        this.damage = 22;
        this.ammoType = AmmoType.T_IGAUSS_HEAVY;
        this.minimumRange = 3;
        this.shortRange = 6;
        this.mediumRange = 12;
        this.longRange = 19;
        this.extremeRange = 24;
        this.flags |= F_SPLITABLE;
        this.tonnage = 20.0f;
        this.criticals = 11;
        this.bv = 385;
        this.cost = 700000;
        this.shortAV = 22;
        this.medAV = 22;
        this.longAV = 12;
        this.maxRange = RANGE_LONG;
        this.explosionDamage = 30;
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
        return new HGRHandler(toHit, waa, game, server);
    }

}

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
public class ISHGaussRifle extends GaussWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = -2379383217525139478L;

    /**
     * 
     */
    public ISHGaussRifle() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "Heavy Gauss Rifle";
        this.setInternalName("ISHeavyGaussRifle");
        this.addLookupName("IS Heavy Gauss Rifle");
        this.heat = 2;
        this.damage = DAMAGE_VARIABLE;
        this.ammoType = AmmoType.T_GAUSS_HEAVY;
        this.minimumRange = 4;
        this.shortRange = 6;
        this.mediumRange = 13;
        this.longRange = 20;
        this.extremeRange = 26;
        this.damageShort = 25;
        this.damageMedium = 20;
        this.damageLong = 10;
        this.flags |= F_SPLITABLE;
        this.tonnage = 18.0f;
        this.criticals = 11;
        this.bv = 346;
        this.cost = 500000;
        this.shortAV = 25;
        this.medAV = 20;
        this.longAV = 10;
        this.maxRange = RANGE_LONG;
        this.explosionDamage = 25;
    }

    public int getDamage(int range) {
        if ( range <= shortRange )
            return damageShort;
        
        if ( range <= mediumRange )
            return damageMedium;
        
            return damageLong;
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

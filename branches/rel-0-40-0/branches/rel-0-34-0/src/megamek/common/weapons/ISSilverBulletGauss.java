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
 * Created on Oct 15, 2004
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
public class ISSilverBulletGauss extends GaussWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = -6873790245999096707L;

    /**
     * 
     */
    public ISSilverBulletGauss() {
        super();
        this.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        this.name = "Silver Bullet Gauss Rifle";
        this.setInternalName("ISSBGR");
        this.addLookupName("IS Silver Bullet Gauss Rifle");
        this.addLookupName("ISSBGaussRifle");
        this.heat = 1;
        this.damage = 15;
        this.rackSize = 15;
        this.minimumRange = 2;
        this.shortRange = 7;
        this.mediumRange = 15;
        this.longRange = 22;
        this.extremeRange = 30;
        this.tonnage = 15.0f;
        this.criticals = 7;
        this.bv = 198;
        this.cost = 350000;
        this.shortAV = 9;
        this.medAV = 9;
        this.longAV = 9;
        this.maxRange = RANGE_LONG;
        this.ammoType = AmmoType.T_SBGAUSS;
        this.flags |= F_NO_AIM;
        this.atClass = CLASS_LBX_AC;
        this.explosionDamage = 20;
   }
    
    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     *      megamek.common.actions.WeaponAttackAction, megamek.common.IGame)
     */
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, IGame game, Server server) {
        return new LBXHandler(toHit, waa, game, server);
    }


}

/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
 * @author Sebastian Brocks
 */
public class ISGaussRiflePrototype extends GaussWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 317770140657000258L;

    /**
     * 
     */
    public ISGaussRiflePrototype() {
        super();
        this.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        this.name = "Gauss Rifle Prototype";
        this.setInternalName("ISGaussRiflePrototype");
        this.addLookupName("IS Gauss Rifle Prototype");
        this.heat = 1;
        this.damage = 15;
        this.ammoType = AmmoType.T_GAUSS;
        this.minimumRange = 2;
        this.shortRange = 7;
        this.mediumRange = 15;
        this.longRange = 22;
        this.extremeRange = 30;
        this.tonnage = 15.0f;
        this.bv = 320;
        this.cost = 300000;
        this.criticals = 8;
        this.flags |= F_PROTOTYPE;
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
        return new PrototypeGaussHandler(toHit, waa, game, server);
    }
}

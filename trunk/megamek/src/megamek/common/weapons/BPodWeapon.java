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
 * Created on Sep 24, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.IGame;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Jason Tighe
 */
public abstract class BPodWeapon extends AmmoWeapon {

    
    /**
     * 
     */
    private static final long serialVersionUID = 654643305102487115L;

    /**
     * 
     */
    public BPodWeapon() {
        super();
        this.heat = 0;
        this.damage = 1;
        this.ammoType = AmmoType.T_BPOD;
        this.rackSize = 1;
        this.minimumRange = 0;
        this.shortRange = 0;
        this.mediumRange = 0;
        this.longRange = 0;
        this.extremeRange = 0;
        this.tonnage = 1.0f;
        this.criticals = 1;
        this.flags |=  F_ONESHOT | F_B_POD;
        this.explosive = true; 
        this.bv = 5;
        this.cost = 6000;
        this.explosionDamage = 2;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     *      megamek.common.actions.WeaponAttackAction, megamek.common.IGame)
     */
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        return new BPodHandler(toHit, waa, game, server);
    }
}

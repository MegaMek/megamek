/**
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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

import megamek.common.Game;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;


/**
 * @author Andrew Hunter
 *
 */
public abstract class MissileWeapon extends AmmoWeapon {
	protected int numMissiles;
	protected int damagePerMissile;
	/**
	 * @return Returns the damagePerMissile.
	 */
	public int getDamagePerMissile() {
		return damagePerMissile;
	}
	protected int damageCluster;

    /**
     * 
     */
    public MissileWeapon() {
        super();
        this.damage = DAMAGE_MISSILE;
    }
    

    /* (non-Javadoc)
     * @see megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData, megamek.common.actions.WeaponAttackAction, megamek.common.Game, megamek.server.Server)
     */
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, Game game, Server server) {
        return new MissileHandler(toHit, waa, game, server);
    }
	/**
	 * @return Returns the damageCluster.
	 */
	public int getDamageCluster() {
		return damageCluster;
	}
	/**
	 * @return Returns the numMissles.
	 */
	public int getNumMissiles() {
		return numMissiles;
	}
}

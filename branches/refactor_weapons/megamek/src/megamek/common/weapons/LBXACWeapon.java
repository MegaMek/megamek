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
 * Created on Oct 14, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;
import megamek.client.FiringDisplay;

/**
 * @author Andrew Hunter
 *
 */
public class LBXACWeapon extends AmmoWeapon {
	/**
	 * 
	 */
	
	/* (non-Javadoc)
	 * @see megamek.common.weapons.Weapon#calcMods(megamek.common.Game, megamek.common.Targetable, int, int, megamek.common.Entity, int, int, megamek.common.Entity, megamek.common.Mounted, megamek.common.WeaponType, boolean, boolean, megamek.common.AmmoType, boolean, megamek.common.Entity, int, megamek.common.LosEffects, megamek.common.ToHitData, int)
	 */
	protected ToHitData calcMods(Game game, Targetable target, int attackerId,
			int weaponId, Entity ae, int aimingAt, int aimingMode, Entity te,
			Mounted weapon, WeaponType wtype, boolean isAttackerInfantry,
			boolean usesAmmo, AmmoType atype, boolean isIndirect,
			Entity spotter, int targEl, LosEffects los, ToHitData losMods,
			int distance) {
		ToHitData toHit=new ToHitData();
		if (aimingMode == FiringDisplay.AIM_MODE_TARG_COMP &&
			aimingAt != Mech.LOC_NONE) {
				toHit.addModifier(3, "aiming with targeting computer");
				} else {
				        if ( ae.hasTargComp() && (atype.getMunitionType() != AmmoType.M_CLUSTER) ) {
				            toHit.addModifier(-1, "targeting computer");
				        }
				      }
		
		toHit.append(super.calcMods(game, target, attackerId, weaponId, ae, aimingAt,
				aimingMode, te, weapon, wtype, isAttackerInfantry, usesAmmo,
				atype, isIndirect, spotter, targEl, los, losMods, distance));
		return toHit;
	}
	/* (non-Javadoc)
	 * @see megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData, megamek.common.actions.WeaponAttackAction, megamek.common.Game, megamek.server.Server)
	 */
	protected AttackHandler getCorrectHandler(ToHitData toHit,
			WeaponAttackAction waa, Game game, Server server) {
		switch(((AmmoType)(game.getEntity(waa.getEntityId()).getEquipment(waa.getAmmoId()).getType())).getMunitionType()) {
			case AmmoType.M_CLUSTER:
				return new LBXHandler(toHit,waa,game,server);
			default:
				return super.getCorrectHandler(toHit, waa, game, server);
		}
	}
	public LBXACWeapon() {
		super();
        this.flags |= WeaponType.F_DIRECT_FIRE;
        this.ammoType = AmmoType.T_AC_LBX;
	}
}

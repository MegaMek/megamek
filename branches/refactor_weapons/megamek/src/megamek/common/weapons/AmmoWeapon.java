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
 * Created on Sep 24, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.*;
import megamek.common.actions.*;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 *
 */
public abstract class AmmoWeapon extends Weapon {
	/**
	 * 
	 */
	public AmmoWeapon() {
		super();
	}
	
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
		if(atype.getToHitModifier()!=0) {
			toHit.addModifier(atype.getToHitModifier(),"ammunition modifier");
		}
		
		toHit.append(super.calcMods(game, target, attackerId, weaponId, ae, aimingAt,
				aimingMode, te, weapon, wtype, isAttackerInfantry, usesAmmo,
				atype, isIndirect, spotter, targEl, los, losMods, distance));
		return toHit;
	}
	/* (non-Javadoc)
	 * @see megamek.common.weapons.Weapon#fire(megamek.common.actions.WeaponAttackAction, megamek.common.Game)
	 */
	public AttackHandler fire(WeaponAttackAction waa, Game game,Server server) {
		//Just in case.  Often necessary when/if multiple ammo weapons are fired; if this line not present
		//then when one ammo slots run dry the rest silently don't fire.
		checkAmmo(waa, game);
		return super.fire(waa, game,server);
	}
	/**
	 * 
	 */
	protected void checkAmmo(WeaponAttackAction waa, Game g) {
		Entity ae=waa.getEntity(g);
		Mounted weapon=ae.getEquipment(waa.getWeaponId());
		Mounted ammo=weapon.getLinked();
		if(ammo==null || ammo.getShotsLeft()<1) {
			ae.loadWeapon(weapon);
            ammo = weapon.getLinked();
		}
	}

	/* (non-Javadoc)
	 * @see megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData, megamek.common.actions.WeaponAttackAction, megamek.common.Game)
	 */
	protected AttackHandler getCorrectHandler(ToHitData toHit,
			WeaponAttackAction waa, Game game,Server server) {
		return new AmmoWeaponHandler(toHit, waa, game,server);
	}
	/* (non-Javadoc)
	 * @see megamek.common.weapons.Weapon#impossibilityCheck(megamek.common.Game, megamek.common.Targetable, int, int, megamek.common.Entity, megamek.common.Entity, megamek.common.Mounted, megamek.common.WeaponType, boolean, megamek.common.AmmoType, boolean, megamek.common.ToHitData, megamek.common.LosEffects)
	 */
	protected ToHitData impossibilityCheck(Game game, Targetable target,
			int attackerId, int weaponId, Entity ae, Entity te, Mounted weapon,
			WeaponType wtype, boolean isAttackerInfantry, AmmoType atype,
			boolean targetInBuilding, ToHitData losMods, LosEffects los) {
		Mounted ammo =  weapon.getLinked();
		
		if(ammo==null || ammo.getShotsLeft()==0) {
			return new ToHitData(TargetRoll.IMPOSSIBLE,"out of ammo");
		}
//		 Are we dumping that ammo?
        if ( ammo.isDumping() ) {
            ae.loadWeapon( weapon );
            if ( ammo.getShotsLeft() == 0 || ammo.isDumping() ) {
                return new ToHitData( ToHitData.IMPOSSIBLE,
                                      "Dumping remaining ammo." );
            }
        }
		return super.impossibilityCheck(game, target, attackerId, weaponId, ae,
				te, weapon, wtype, isAttackerInfantry, atype, targetInBuilding,
				losMods, los);
	}
}

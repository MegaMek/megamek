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
 * Created on Sep 25, 2004
 *
 */
package megamek.common.weapons;
import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.client.FiringDisplay;
import megamek.server.Server;
/**
 * @author Andrew Hunter
 * N.B.  This class is overriden for AC/2, AC/5, AC/10, AC/10, NOT ultras/LB/RAC. 
 *  (No difference between ACWeapon and AmmoWeapon except the ability to use special ammos (precision, AP, etc.) )
 */
public class ACWeapon extends AmmoWeapon {
	/**
	 * @param t
	 * @param w
	 * @param g
	 */
	public ACWeapon() {
		super();
        this.flags |= F_DIRECT_FIRE;
        this.ammoType = AmmoType.T_AC;
	}

	protected ToHitData calcMods(Game game, Targetable target, int attackerId,
			int weaponId, Entity ae, int aimingAt, int aimingMode, Entity te,
			Mounted weapon, WeaponType wtype, boolean isAttackerInfantry,
			boolean usesAmmo, AmmoType atype, boolean isIndirect,
			Entity spotter, int targEl, LosEffects los, ToHitData losMods,
			int distance) {
		ToHitData toHit=new ToHitData();
		toHit.append(super.calcMods(game, target, attackerId, weaponId, ae, aimingAt,
				aimingMode, te, weapon, wtype, isAttackerInfantry, usesAmmo,
				atype, isIndirect, spotter, targEl, los, losMods, distance));
		if (aimingMode == FiringDisplay.AIM_MODE_TARG_COMP &&
		          aimingAt != Mech.LOC_NONE) {
		          toHit.addModifier(3, "aiming with targeting computer");
		        } else {
		          if ( ae.hasTargComp() && wtype.hasFlag(WeaponType.F_DIRECT_FIRE) &&
		               (!usesAmmo || atype.getMunitionType() != AmmoType.M_CLUSTER) ) {
		              toHit.addModifier(-1, "targeting computer");
		          }
		        }
		switch(atype.getMunitionType()) {
			case AmmoType.M_ARMOR_PIERCING:
				toHit.addModifier( 1, "Armor-Piercing Ammo" );
			break;
			case AmmoType.M_PRECISION:
				ToHitData thTemp=Compute.getTargetMovementModifier(game,target.getTargetId());
				int nAdjust = Math.min(2, thTemp.getValue());
            	if (nAdjust > 0) {
            		toHit.append(new ToHitData(-nAdjust, "Precision Ammo"));
            	}
            break;            	
		}
		
		return toHit;
	}
	/* (non-Javadoc)
	 * @see megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData, megamek.common.actions.WeaponAttackAction, megamek.common.Game)
	 */
	protected AttackHandler getCorrectHandler(ToHitData toHit,
			WeaponAttackAction waa, Game game,Server server) {
		AmmoType atype=(AmmoType)game.getEntity(waa.getEntityId()).getEquipment(waa.getWeaponId()).getLinked().getType();
		switch(atype.getMunitionType()) {
			case AmmoType.M_ARMOR_PIERCING:
				return new ACAPHandler(toHit,waa,game,server);
			case AmmoType.M_FLECHETTE:
				return new ACFlechetteHandler(toHit,waa,game,server);
			default:
				return super.getCorrectHandler(toHit, waa, game,server);
		}
		
	}
}

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
 * Created on Sep 13, 2004
 *
 */
package megamek.common.weapons;
import megamek.common.*;
import megamek.client.FiringDisplay;
/**
 * @author Andrew Hunter
 *
 */
public abstract class PPCWeapon extends EnergyWeapon {
	/**
	 * 
	 */
	public PPCWeapon() {
		super();
		this.flags |=WeaponType.F_PPC | WeaponType.F_DIRECT_FIRE;
	    ammoType = AmmoType.T_NA;
	}
	protected ToHitData calcMods(Game game, Targetable target, int attackerId, int weaponId, final Entity ae, int aimingAt, int aimingMode, Entity te, final Mounted weapon, final WeaponType wtype, boolean isAttackerInfantry, final boolean usesAmmo, final AmmoType atype, boolean isIndirect, Entity spotter, int targEl, LosEffects los, ToHitData losMods, int distance) {
//		 add targeting computer (except with LBX cluster ammo)
       ToHitData toHit=new ToHitData();
       if (aimingMode == FiringDisplay.AIM_MODE_TARG_COMP &&
         aimingAt != Mech.LOC_NONE) {
         toHit.addModifier(3, "aiming with targeting computer");
       } else {
         if ( ae.hasTargComp()) {
             toHit.addModifier(-1, "targeting computer");
         }
       }
       toHit.append(super.calcMods(game,target,attackerId,weaponId,ae,aimingAt,aimingMode,te,weapon,wtype,isAttackerInfantry,usesAmmo,atype,isIndirect,spotter,targEl,los,losMods,distance));          
       return toHit;
	}
}


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
 * Created on Sep 29, 2004
 *
 */
package megamek.common.weapons;
import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.client.FiringDisplay;
import megamek.server.Server;
/**
 * @author Andrew Hunter
 *
 */
public class UACWeapon extends AmmoWeapon {
	/**
	 * 
	 */
	public UACWeapon() {
		super();
        this.flags |= F_DIRECT_FIRE;
        this.ammoType = AmmoType.T_AC_ULTRA;
        String[] modes = { "Single", "Ultra" };
        this.setModes(modes);
	}
	protected ToHitData calcMods(Game game, Targetable target, int attackerId, int weaponId, final Entity ae, int aimingAt, int aimingMode, Entity te, final Mounted weapon, final WeaponType wtype, boolean isAttackerInfantry, final boolean usesAmmo, final AmmoType atype, boolean isIndirect, Entity spotter, int targEl, LosEffects los, ToHitData losMods, int distance) {
//		 add targeting computer (except with LBX cluster ammo)
      ToHitData toHit=new ToHitData();
      if (aimingMode == FiringDisplay.AIM_MODE_TARG_COMP &&
        aimingAt != Mech.LOC_NONE) {
        toHit.addModifier(3, "aiming with targeting computer");
      } else {
        if ( ae.hasTargComp() && wtype.hasFlag(WeaponType.F_DIRECT_FIRE) &&
             (!usesAmmo || atype.getMunitionType() != AmmoType.M_CLUSTER) ) {
            toHit.addModifier(-1, "targeting computer");
        }
      }
      toHit.append(super.calcMods(game,target,attackerId,weaponId,ae,aimingAt,aimingMode,te,weapon,wtype,isAttackerInfantry,usesAmmo,atype,isIndirect,spotter,targEl,los,losMods,distance));          
      return toHit;
	}
	
	/* (non-Javadoc)
	 * @see megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData, megamek.common.actions.WeaponAttackAction, megamek.common.Game)
	 */
	protected AttackHandler getCorrectHandler(ToHitData toHit,
			WeaponAttackAction waa, Game game, Server server) {
		Mounted weapon=game.getEntity(waa.getEntityId()).getEquipment(waa.getWeaponId());
		if(weapon.curMode()=="Ultra") {
			server.sendServerChat("Getting an ultra Handler.");
			return new UltraWeaponHandler(toHit,waa,game,server);
		} else {
			server.sendServerChat("Getting a normal.");
			return super.getCorrectHandler(toHit, waa, game,server);
		}
	}
}

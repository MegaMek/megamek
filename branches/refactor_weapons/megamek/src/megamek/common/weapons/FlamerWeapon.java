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
 * Created on Sep 23, 2004
 *
 */
package megamek.common.weapons;
import megamek.common.*;
import megamek.common.actions.*;
/**
 * @author Andrew Hunter
 *
 */
public abstract class FlamerWeapon extends EnergyWeapon {
	/**
	 * 
	 */
	public FlamerWeapon() {
		super();
		this.flags |= WeaponType.F_FLAMER;
		this.ammoType = AmmoType.T_NA;
		String modes[] = { "Damage", "Heat" };
		this.setModes(modes);
				
	}
	protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game) {
		if((game.getEntity(waa.getEntityId()).getEquipment(waa.getWeaponId()).curMode())=="Heat") {
			
			return new FlamerHeatHandler(toHit,waa,game);
			
		} else {
				return new WeaponHandler(toHit,waa,game);						
		}
	}
}

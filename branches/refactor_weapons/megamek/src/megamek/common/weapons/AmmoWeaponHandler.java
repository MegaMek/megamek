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
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;

/**
 * @author Andrew Hunter
 *
 */
public class AmmoWeaponHandler extends WeaponHandler {
	Mounted ammo;
	/**
	 * @param t
	 * @param w
	 * @param g
	 */
	public AmmoWeaponHandler(ToHitData t, WeaponAttackAction w, Game g) {
		super(t, w, g);
		
	}
	/* (non-Javadoc)
	 * @see megamek.common.weapons.WeaponHandler#addHeatUseAmmo()
	 */
	protected void addHeatUseAmmo() {
		checkAmmo();
		if(ammo==null) {//Can't happen.  w/o legal ammo, the weapon *shouldn't* fire.
			System.out.println("Handler can't find any ammo!  Oh no!");
		}
		if (ammo.getShotsLeft() <= 0) {
            ae.loadWeapon(weapon);
            ammo = weapon.getLinked();
        }
        ammo.setShotsLeft(ammo.getShotsLeft() - 1);
		super.addHeatUseAmmo();
	}
	protected void checkAmmo() {
		ammo=weapon.getLinked();
		if(ammo==null) {
			ae.loadWeapon(weapon);
            ammo = weapon.getLinked();
		}
	}
}

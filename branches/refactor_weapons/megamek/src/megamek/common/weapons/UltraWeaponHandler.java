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

import megamek.common.Game;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;

/**
 * @author Andrew Hunter
 *
 */
public class UltraWeaponHandler extends AmmoWeaponHandler {
	int howManyShots;
	/**
	 * @param t
	 * @param w
	 * @param g
	 */
	public UltraWeaponHandler(ToHitData t, WeaponAttackAction w, Game g) {
		super(t, w, g);
	}
	
	/* (non-Javadoc)
	 * @see megamek.common.weapons.WeaponHandler#addHeatUseAmmo()
	 */
	protected void addHeatUseAmmo() {
		super.addHeatUseAmmo();
	}
	/* (non-Javadoc)
	 * @see megamek.common.weapons.WeaponHandler#allShotsHit()
	 */
	protected boolean allShotsHit() {
		return super.allShotsHit();
	}
	/* (non-Javadoc)
	 * @see megamek.common.weapons.WeaponHandler#calcHits()
	 */
	protected int calcHits() {
		return Compute.missilesHit(howManyShots)
	}
}

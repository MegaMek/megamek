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
 * Created on Oct 15, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.Compute;
import megamek.common.Game;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 *
 */
public class LBXHandler extends AmmoWeaponHandler {
	/**
	 * @param t
	 * @param w
	 * @param g
	 * @param s
	 */
	public LBXHandler(ToHitData t, WeaponAttackAction w, Game g, Server s) {
		super(t, w, g, s);
	}
	
	/* (non-Javadoc)
	 * @see megamek.common.weapons.WeaponHandler#allShotsHit()
	 */
	protected boolean allShotsHit() {
		return super.allShotsHit();
	}
	/* (non-Javadoc)
	 * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
	 */
	protected int calcDamagePerHit() {
		return 1;
	}
	/* (non-Javadoc)
	 * @see megamek.common.weapons.WeaponHandler#calcHits()
	 */
	protected int calcHits() {
		return allShotsHit()? wtype.getRackSize() : Compute.missilesHit(wtype.getRackSize());
	}
}

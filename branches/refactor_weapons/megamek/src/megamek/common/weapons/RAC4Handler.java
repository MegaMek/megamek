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
public class RAC4Handler extends UltraWeaponHandler {
	/**
	 * @param t
	 * @param w
	 * @param g
	 * @param s
	 */
	public RAC4Handler(ToHitData t, WeaponAttackAction w, Game g, Server s) {
		super(t, w, g, s);
	}
	
	/* (non-Javadoc)
	 * @see megamek.common.weapons.WeaponHandler#doChecks()
	 */
	protected void doChecks() {
		boolean jams=false;
		switch(howManyShots) {
			case 4:
				if(roll<=3) {
					jams=true;
				}
				break;
			case 2:
				if(roll<=2) {
					jams=true;
				}
				break;
		}
		if(jams) {
			game.getPhaseReport().append(" AND THE AUTOCANNON JAMS."); 
			weapon.setJammed(true);
		}
	}
	/* (non-Javadoc)
	 * @see megamek.common.weapons.WeaponHandler#useAmmo()
	 */
	protected void useAmmo() {
		int shotsFromPreviousAmmos=0,shotsNeedFiring;
		setDone();       
		checkAmmo();
		int total =ae.getTotalAmmoOfType(ammo.getType());
		howManyShots=1;
		if(total>=4) {
			howManyShots=4;
		}
		if(total>=2) {
			howManyShots=2;			
		}
		shotsNeedFiring=howManyShots;
		if(ammo.getShotsLeft()==0) {
			ae.loadWeapon(weapon);
			ammo = weapon.getLinked();
			//there will be some ammo somewhere, otherwise shot will not have been fired.
		}
		
		while(shotsNeedFiring>ammo.getShotsLeft()) {
			shotsFromPreviousAmmos+=ammo.getShotsLeft();
			shotsNeedFiring-=ammo.getShotsLeft();
			ammo.setShotsLeft(0);
			ae.loadWeapon(weapon);
			ammo = weapon.getLinked();
		}
		ammo.setShotsLeft(ammo.getShotsLeft()-shotsNeedFiring);
	}
}

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
		howManyShots=2;
	}
	
	/* (non-Javadoc)
	 * @see megamek.common.weapons.WeaponHandler#addHeatUseAmmo()
	 */
	protected void addHeatUseAmmo() {
		checkAmmo();
		if(ammo.getShotsLeft()==0) {
			//Ugh, we need a new ammo!
			ae.loadWeapon(weapon);
			ammo = weapon.getLinked();
			//there will be some ammo somewhere, otherwise shot will not have been fired.
		}
		if(ammo.getShotsLeft()==1)  {
			//we need to revert.
			howManyShots=1;
		}
		ammo.setShotsLeft(ammo.getShotsLeft() - howManyShots);
		addHeat();
		setDone();        
	}
	/* (non-Javadoc)
	 * @see megamek.common.weapons.WeaponHandler#allShotsHit()
	 */
	protected boolean allShotsHit() {
		if( ( target.getTargetType() == Targetable.TYPE_BLDG_IGNITE ||
                 target.getTargetType() == Targetable.TYPE_BUILDING ) &&
               ae.getPosition().distance( target.getPosition() ) <= 1 ) {
			return true;
		}
		return false;
	}
	/* (non-Javadoc)
	 * @see megamek.common.weapons.WeaponHandler#calcHits()
	 */
	protected int calcHits() {
		int shotsHit;
		switch(howManyShots) {//necessary, missilesHit not defined for missiles=1
			case 2:
				shotsHit = allShotsHit()? 2:Compute.missilesHit(howManyShots);
			default:
				shotsHit = 1;
		}
		if(howManyShots==2) {
			game.getPhaseReport().append("Hits with " + shotsHit + " shot(s)\n");
		}
		return shotsHit;
	}
	
	/* (non-Javadoc)
	 * @see megamek.common.weapons.WeaponHandler#addHeat()
	 */
	protected void addHeat() {
		switch(howManyShots) {//silly hack
			case 2:
				super.addHeat();
			case 1:
				super.addHeat();
				break;
				
		}
	}
	/* (non-Javadoc)
	 * @see megamek.common.weapons.WeaponHandler#doChecks()
	 */
	protected void doChecks() {
		super.doChecks();
		if(roll==2 && howManyShots==2) {
			game.getPhaseReport().append(" AND THE AUTOCANNON JAMS."); 
			weapon.setJammed(true);
		}
	}
}

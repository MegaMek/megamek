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
import megamek.server.Server;
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
	public UltraWeaponHandler(ToHitData t, WeaponAttackAction w, Game g,Server s) {
		super(t, w, g, s);
	}
	
	/* (non-Javadoc)
	 * @see megamek.common.weapons.WeaponHandler#addHeatUseAmmo()
	 */
	protected void useAmmo() {
		setDone();       
		checkAmmo();
		int total =ae.getTotalAmmoOfType(ammo.getType());
		if(total>1) {
			howManyShots=2;
		}
		if(total==1) {
			howManyShots=1;
		}
		if(total==0) {
			//can't happen?
			
		}
		if(ammo.getShotsLeft()==0) {
			ae.loadWeapon(weapon);
			ammo = weapon.getLinked();
			//there will be some ammo somewhere, otherwise shot will not have been fired.
		}
		if(ammo.getShotsLeft()==1)  {
			ammo.setShotsLeft(0);
			ae.loadWeapon(weapon);
			ammo = weapon.getLinked();
			//that fired one, do we need to fire another?
			ammo.setShotsLeft(ammo.getShotsLeft() - ((howManyShots==2)? 1 : 0));
		} else {
			ammo.setShotsLeft(ammo.getShotsLeft() - howManyShots);
		}
		
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
			case 1:
				shotsHit = 1;
				break;
			default:
				shotsHit = allShotsHit()? howManyShots:Compute.missilesHit(howManyShots);
				game.getPhaseReport().append("Hits with " + shotsHit + " shot(s)\n");
				break;
				
		}
		return shotsHit;
	}
	
	/* (non-Javadoc)
	 * @see megamek.common.weapons.WeaponHandler#addHeat()
	 */
	protected void addHeat() {//silly hack
		for(int x=0;x<howManyShots;x++) {
			super.addHeat();
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

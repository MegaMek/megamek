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
}

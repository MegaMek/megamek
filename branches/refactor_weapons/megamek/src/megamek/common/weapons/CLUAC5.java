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
 * Created on Oct 2, 2004
 *
 */
package megamek.common.weapons;

/**
 * @author Andrew Hunter
 *
 */
public class CLUAC5 extends UACWeapon {
	/**
	 * 
	 */
	public CLUAC5() {
		super();
		this.name = "Ultra AC/5";
        this.setInternalName("CLUltraAC5");
        this.addLookupName("Clan Ultra AC/5");
        this.heat = 1;
        this.damage = 5;
        this.rackSize = 5;
        this.minimumRange = 0;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 21;
        this.tonnage = 7.0f;
        this.criticals = 3;
        this.bv = 123;
	}
}

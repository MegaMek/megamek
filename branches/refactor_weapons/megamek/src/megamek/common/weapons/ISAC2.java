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

/**
 * @author Andrew Hunter
 *
 */
public class ISAC2 extends ACWeapon {
	/**
	 * 
	 */
	public ISAC2() {
		super();
		this.name = "Auto Cannon/2";
        this.setInternalName(this.name);
        this.addLookupName("IS Auto Cannon/2");
        this.addLookupName("ISAC2");
        this.addLookupName("IS Autocannon/2");
        this.heat = 1;
        this.damage = 2;
        this.rackSize = 2;
        this.minimumRange = 4;
        this.shortRange = 8;
        this.mediumRange = 16;
        this.longRange = 24;
        this.tonnage = 6.0f;
        this.criticals = 1;
        this.bv = 37;
	}
}

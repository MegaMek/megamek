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

/**
 * @author Andrew Hunter
 *
 */
public class ISLB5XAC extends LBXACWeapon {
	/**
	 * 
	 */
	public ISLB5XAC() {
		super();
		this.name = "LB 5-X AC";
        this.setInternalName("ISLBXAC5");
        this.addLookupName("IS LB 5-X AC");
        this.heat = 1;
        this.damage = 5;
        this.rackSize = 5;
        this.minimumRange = 3;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 21;
        this.tonnage = 8.0f;
        this.criticals = 5;
        this.bv = 83;
	}
}

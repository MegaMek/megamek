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
public class ISLB2XAC extends LBXACWeapon {
	/**
	 * 
	 */
	public ISLB2XAC() {
		super();
		this.name = "LB 2-X AC";
        this.setInternalName("ISLBXAC2");
        this.addLookupName("IS LB 2-X AC");
        this.heat = 1;
        this.damage = 2;
        this.rackSize = 2;
        this.minimumRange = 4;
        this.shortRange = 9;
        this.mediumRange = 18;
        this.longRange = 27;
        this.tonnage = 6.0f;
        this.criticals = 4;
        this.bv = 42;
	}
}

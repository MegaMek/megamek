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
 * Created on Oct 1, 2004
 *
 */
package megamek.common.weapons;

/**
 * @author Andrew Hunter
 *
 */
public class ISUAC5 extends UACWeapon {
	/**
	 * 
	 */
	public ISUAC5() {
		super(); 
		this.name = "Ultra AC/5";
        this.setInternalName("ISUltraAC5");
        this.addLookupName("IS Ultra AC/5");
        this.heat = 1;
        this.damage = 5;
        this.rackSize = 5;
        this.minimumRange = 2;
        this.shortRange = 6;
        this.mediumRange = 13;
        this.longRange = 20;
        this.tonnage = 9.0f;
        this.criticals = 5;
        this.bv = 113;
	}
}

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
 * Created on Sep 13, 2004
 *
 */
package megamek.common.weapons;

/**
 * @author Andrew Hunter
 *
 */
public class CLERPPC extends PPCWeapon {
	/**
	 * 
	 */
	public CLERPPC() {
		super();
		this.name = "ER PPC";
        this.setInternalName("CLERPPC");
        this.addLookupName("Clan ER PPC");
        this.heat = 15;
        this.damage = 15;
        this.minimumRange = 0;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 23;
        this.waterShortRange = 4;
        this.waterMediumRange = 10;
        this.waterLongRange = 16;
        this.tonnage = 6.0f;
        this.criticals = 2;
        this.bv = 412;

	}
}

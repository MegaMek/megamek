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
public class ISPPC extends PPCWeapon {
	/**
	 * 
	 */
	public ISPPC() {
		super();
		this.name = "Particle Cannon";
        this.setInternalName(this.name);
        this.addLookupName("IS PPC");
        this.addLookupName("ISPPC");
        this.heat = 10;
        this.damage = 10;
        this.minimumRange = 3;
        this.shortRange = 6;
        this.mediumRange = 12;
        this.longRange = 18;
        this.waterShortRange = 4;
        this.waterMediumRange = 7;
        this.waterLongRange = 10;
        this.tonnage = 7.0f;
        this.criticals = 3;
        this.bv = 176;
	}
}

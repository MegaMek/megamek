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
public class CLLB20XAC extends LBXACWeapon {
	/**
	 * 
	 */
	public CLLB20XAC() {
		super();
		this.name = "LB 20-X AC";
        this.setInternalName("CLLBXAC20");
        this.addLookupName("Clan LB 20-X AC");
        this.heat = 6;
        this.damage = 20;
        this.rackSize = 20;
        this.minimumRange = 0;
        this.shortRange = 4;
        this.mediumRange = 8;
        this.longRange = 12;
        this.tonnage = 12.0f;
        this.criticals = 9;
        this.bv = 237;
	}
}

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
 * Created on Oct 19, 2004
 *
 */
package megamek.common.weapons;
import megamek.common.*;
/**
 * @author Andrew Hunter
 *
 */
public class ISGaussRifle extends GaussWeapon {
	/**
	 * 
	 */
	public ISGaussRifle() {
		super();
		this.name = "Gauss Rifle";
        this.setInternalName("ISGaussRifle");
        this.addLookupName("IS Gauss Rifle");
        this.heat = 1;
        this.damage = 15;
        this.ammoType = AmmoType.T_GAUSS;
        this.minimumRange = 2;
        this.shortRange = 7;
        this.mediumRange = 15;
        this.longRange = 22;
        this.tonnage = 15.0f;
        this.criticals = 7;
        this.bv = 321;
	}
}

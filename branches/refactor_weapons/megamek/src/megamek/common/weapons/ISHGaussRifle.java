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
public class ISHGaussRifle extends GaussWeapon {
	/**
	 * 
	 */
	public ISHGaussRifle() {
		super();
		this.name = "Heavy Gauss Rifle";
        this.setInternalName("ISHeavyGaussRifle");
        this.addLookupName("IS Heavy Gauss Rifle");
        this.heat = 2;
        this.damage = DAMAGE_VARIABLE;
        this.ammoType = AmmoType.T_GAUSS_HEAVY;
        this.minimumRange = 4;
        this.shortRange = 6;
        this.mediumRange = 13;
        this.longRange = 20;
        this.tonnage = 18.0f;
        this.criticals = 11;
        this.bv = 346;
	}
}

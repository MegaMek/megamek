/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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

import megamek.common.AmmoType;
import megamek.common.TechConstants;

/**
 * @author Andrew Hunter
 */
public class ISLGaussRifle extends GaussWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 8971550996626387100L;

    /**
     * 
     */
    public ISLGaussRifle() {
        super();
        this.techLevel = TechConstants.T_IS_LEVEL_2;
        this.name = "Light Gauss Rifle";
        this.setInternalName("ISLightGaussRifle");
        this.addLookupName("IS Light Gauss Rifle");
        this.heat = 1;
        this.damage = 8;
        this.ammoType = AmmoType.T_GAUSS_LIGHT;
        this.minimumRange = 3;
        this.shortRange = 8;
        this.mediumRange = 17;
        this.longRange = 25;
        this.extremeRange = 34;
        this.tonnage = 12.0f;
        this.criticals = 5;
        this.bv = 159;
        this.cost = 275000;
    }
}

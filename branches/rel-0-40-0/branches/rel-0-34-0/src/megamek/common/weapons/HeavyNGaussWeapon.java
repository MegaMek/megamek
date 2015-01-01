/* MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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

import megamek.common.AmmoType;
import megamek.common.TechConstants;

/**
 * @author Jay Lawson
 */
public class HeavyNGaussWeapon extends NavalGaussWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 8756042527483383101L;

    /**
     * 
     */
    public HeavyNGaussWeapon() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "Heavy N-Gauss";
        this.setInternalName(this.name);
        this.addLookupName("HeavyNGauss");
        this.heat = 18;
        this.damage = 30;
        this.ammoType = AmmoType.T_HEAVY_NGAUSS;
        this.shortRange = 12;
        this.mediumRange = 24;
        this.longRange = 36;
        this.extremeRange = 48;
        this.tonnage = 7-00.0f;
        this.bv = 6048;
        this.cost = 50050000;
        this.shortAV = 30;
        this.medAV = 30;
        this.longAV = 30;
        this.extAV = 30;
        this.maxRange = RANGE_EXT;
    }
}

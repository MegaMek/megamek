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
 * Created on Sep 24, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class ISSupportPPC extends PPCWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = -993141316216102914L;

    /**
     * 
     */
    public ISSupportPPC() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "Support PPC";
        this.setInternalName(this.name);
        this.addLookupName("ISSupportPPC");
        this.damage = 2;
        this.ammoType = AmmoType.T_NA;
        this.shortRange = 2;
        this.mediumRange = 5;
        this.longRange = 7;
        this.extremeRange = 10;
        this.bv = 14;
        this.setModes(new String[] { "Field Inhibitor ON",
                "Field Inhibitor OFF" });
    }
}

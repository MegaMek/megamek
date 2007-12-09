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
 * @author Andrew Hunter
 * 
 */
public class ISMediumRecoillessRifle extends Weapon {
    /**
     * 
     */
    public ISMediumRecoillessRifle() {
        super();
        this.techLevel = TechConstants.T_IS_LEVEL_2;
        this.name = "Medium Recoilless";
        this.setInternalName(this.name);
        this.addLookupName("ISMedium Recoilless Rifle");
        this.damage = 3;
        this.ammoType = AmmoType.T_NA;
        this.shortRange = 2;
        this.mediumRange = 4;
        this.longRange = 6;
        this.extremeRange = 8;
        this.bv = 19;
        this.flags |= F_DIRECT_FIRE | F_NO_FIRES | F_BALLISTIC;
    }
}

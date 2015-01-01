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

import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class ISMicroGrenadeLauncher extends Weapon {
    /**
     * 
     */
    private static final long serialVersionUID = 5856065014622975919L;

    /**
     * 
     */
    public ISMicroGrenadeLauncher() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "Micro Grenade Launcher";
        this.setInternalName(this.name);
        this.addLookupName("ISMicroGrenadeLauncher");
        this.heat = 0;
        this.damage = 1;
        this.minimumRange = WEAPON_NA;
        this.shortRange = 1;
        this.mediumRange = 2;
        this.longRange = 2;
        this.extremeRange = 4;
        this.tonnage = 0.0f;
        this.criticals = 0;
        this.bv = 0;
        this.flags |= F_BALLISTIC;
    }
}

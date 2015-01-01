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
 * Created on Sep 25, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.TechConstants;

/**
 * @author Andrew Hunter
 */
public class ISRailGun extends GaussWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 8879671694066711976L;

    /**
     * 
     */
    public ISRailGun() {
        super();
        this.techLevel = TechConstants.T_IS_UNOFFICIAL;
        this.name = "Rail Gun";
        this.setInternalName("ISRailGun");
        this.addLookupName("IS Rail Gun");
        this.heat = 1;
        this.damage = 22;
        this.ammoType = AmmoType.T_RAIL_GUN;
        this.minimumRange = 1;
        this.shortRange = 6;
        this.mediumRange = 13;
        this.longRange = 19;
        this.extremeRange = 26;
        this.tonnage = 18.0f;
        this.criticals = 9;
        this.bv = 411;
        this.cost = 300000;
        this.explosionDamage = 20;
    }
}

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
 * Created on Sep 7, 2005
 *
 */
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class ISC3M extends TAGWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -8367068184993071837L;

    public ISC3M() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "C3 Master with TAG";
        this.setInternalName("ISC3MasterUnit");
        this.addLookupName("IS C3 Computer");
        this.addLookupName("ISC3MasterComputer");
        this.tonnage = 5;
        this.criticals = 5;
        this.hittable = true;
        this.spreadable = false;
        this.cost = 1500000;
        this.bv = 0;
        this.flags |= F_C3M;
        this.heat = 0;
        this.damage = 0;
        this.shortRange = 5;
        this.mediumRange = 10;
        this.longRange = 15;
        this.extremeRange = 20;
    }
}

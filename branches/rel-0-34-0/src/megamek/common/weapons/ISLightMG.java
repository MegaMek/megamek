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
 * Created on Oct 20, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class ISLightMG extends MGWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 8148848145274790948L;

    /**
     * 
     */
    public ISLightMG() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "Light Machine Gun";
        this.setInternalName(this.name);
        this.addLookupName("IS Light Machine Gun");
        this.addLookupName("ISLightMG");
        this.ammoType = AmmoType.T_MG_LIGHT;
        this.heat = 0;
        this.damage = 1;
        this.rackSize = 1;
        this.shortRange = 2;
        this.mediumRange = 4;
        this.longRange = 6;
        this.extremeRange = 8;
        this.tonnage = 0.5f;
        this.criticals = 1;
        this.bv = 5;
        this.cost = 5000;
        this.shortAV = 1;
        this.maxRange = RANGE_SHORT;
        
        this.atClass = CLASS_AC;
    }

}

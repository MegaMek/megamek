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
public class ISHeavyMG extends MGWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 9170779598178306077L;

    /**
     * 
     */
    public ISHeavyMG() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "Heavy Machine Gun";
        this.setInternalName(this.name);
        this.addLookupName("IS Heavy Machine Gun");
        this.addLookupName("ISHeavyMG");
        this.ammoType = AmmoType.T_MG_HEAVY;
        this.heat = 0;
        this.damage = 3;
        this.rackSize = 3;
        this.shortRange = 1;
        this.mediumRange = 2;
        this.longRange = 2;
        this.extremeRange = 2;
        this.tonnage = 1.0f;
        this.criticals = 1;
        this.bv = 6;
        this.cost = 7500;
        this.shortAV = 3;
        this.maxRange = RANGE_SHORT;
    }

}

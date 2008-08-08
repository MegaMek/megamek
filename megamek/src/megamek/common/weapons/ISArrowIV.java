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
public class ISArrowIV extends ArtilleryWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -4495524659692575107L;

    /**
     * 
     */
    public ISArrowIV() {
        super();
        this.techLevel = TechConstants.T_IS_LEVEL_2;
        this.name = "Arrow IV";
        this.setInternalName("ISArrowIV");
        this.addLookupName("ISArrowIVSystem");
        this.addLookupName("IS Arrow IV System");
        this.addLookupName("IS Arrow IV Missile System");
        this.heat = 10;
        this.rackSize = 20;
        this.ammoType = AmmoType.T_ARROW_IV;
        this.shortRange = 1;
        this.mediumRange = 2;
        this.longRange = 8;
        this.extremeRange = 8; // No extreme range.
        this.tonnage = 15f;
        this.criticals = 15;
        this.bv = 171;
        this.cost = 450000;
    }

}

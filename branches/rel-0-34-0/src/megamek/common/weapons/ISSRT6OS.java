/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class ISSRT6OS extends SRTWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -1788634690534985124L;

    /**
     * 
     */
    public ISSRT6OS() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "SRT 6 (OS)";
        this.setInternalName("ISSRT6OS");
        this.addLookupName("ISSRT6 (OS)"); // mtf
        this.addLookupName("IS SRT 6 (OS)"); // tdb
        this.addLookupName("OS SRT-6"); // mep
        this.heat = 4;
        this.rackSize = 6;
        this.waterShortRange = 3;
        this.waterMediumRange = 6;
        this.waterLongRange = 9;
        this.waterExtremeRange = 12;
        this.tonnage = 3.5f;
        this.criticals = 2;
        this.bv = 12;
        this.flags |= F_NO_FIRES | F_ONESHOT;
        this.cost = 80000;
    }
}

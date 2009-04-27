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

import megamek.common.AmmoType;
import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class ISImprovedNarcOS extends NarcWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -3509295242151016719L;

    /**
     * 
     */
    public ISImprovedNarcOS() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "iNarc (OS)";
        this.setInternalName("ISImprovedNarc (OS)");
        this.addLookupName("IS OS iNarc Beacon");
        this.addLookupName("IS iNarc Missile Beacon (OS)");
        this.ammoType = AmmoType.T_INARC;
        this.heat = 0;
        this.rackSize = 1;
        this.shortRange = 4;
        this.mediumRange = 9;
        this.longRange = 15;
        this.extremeRange = 18;
        this.tonnage = 5.5f;
        this.criticals = 2;
        this.bv = 15;
        this.flags |= F_ONESHOT;
        this.cost = 250000;
    }
}

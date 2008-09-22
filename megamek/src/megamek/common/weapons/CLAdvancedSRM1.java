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
public class CLAdvancedSRM1 extends AdvancedSRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -6709245485008575199L;

    /**
     * 
     */
    public CLAdvancedSRM1() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "Advanced SRM 1";
        this.setInternalName("CLAdvancedSRM1");
        this.addLookupName("Clan Advanced SRM-1");
        this.addLookupName("Clan Advanced SRM 1");
        this.rackSize = 1;
        this.shortRange = 4;
        this.mediumRange = 8;
        this.longRange = 12;
        this.extremeRange = 16;
        this.bv = 15;
        this.flags |= F_NO_FIRES;
        this.cost = 80000;
    }
}

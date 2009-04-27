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
public class CLAdvancedSRM6 extends AdvancedSRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -6710415894545970512L;

    /**
     * 
     */
    public CLAdvancedSRM6() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "Advanced SRM 6";
        this.setInternalName("CLAdvancedSRM6");
        this.addLookupName("Clan Advanced SRM-6");
        this.addLookupName("Clan Advanced SRM 6");
        this.rackSize = 6;
        this.shortRange = 4;
        this.mediumRange = 8;
        this.longRange = 12;
        this.extremeRange = 16;
        this.bv = 90;
        this.flags |= F_NO_FIRES;
        this.cost = 80000;
    }
}

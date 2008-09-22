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
public class CLAdvancedSRM4 extends AdvancedSRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 1382352551382640865L;

    /**
     * 
     */
    public CLAdvancedSRM4() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "Advanced SRM 4";
        this.setInternalName("CLAdvancedSRM4");
        this.addLookupName("Clan Advanced SRM-4");
        this.addLookupName("Clan Advanced SRM 4");
        this.rackSize = 4;
        this.shortRange = 4;
        this.mediumRange = 8;
        this.longRange = 12;
        this.extremeRange = 16;
        this.bv = 60;
        this.flags |= F_NO_FIRES;
        this.cost = 80000;
    }
}

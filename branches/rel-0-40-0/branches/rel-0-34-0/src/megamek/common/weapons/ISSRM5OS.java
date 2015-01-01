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
public class ISSRM5OS extends SRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 2564548381701365334L;

    /**
     * 
     */
    public ISSRM5OS() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "SRM 5 (OS)";
        this.setInternalName(this.name);
        this.addLookupName("ISSRM5OS");
        this.rackSize = 5;
        this.shortRange = 3;
        this.mediumRange = 6;
        this.longRange = 9;
        this.extremeRange = 12;
        this.bv = 9;
        this.flags |= F_NO_FIRES | F_ONESHOT;
    }
}

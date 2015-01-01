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
public class ISSRM3OS extends SRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 8732407650030864483L;

    /**
     * 
     */
    public ISSRM3OS() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "SRM 3 (OS)";
        this.setInternalName(this.name);
        this.addLookupName("ISSRM3OS");
        this.rackSize = 3;
        this.shortRange = 3;
        this.mediumRange = 6;
        this.longRange = 9;
        this.extremeRange = 12;
        this.bv = 6;
        this.flags |= F_NO_FIRES | F_ONESHOT;
    }
}

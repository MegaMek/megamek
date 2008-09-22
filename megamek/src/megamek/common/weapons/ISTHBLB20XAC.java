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
 * Created on Oct 15, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.TechConstants;

/**
 * @author Andrew Hunter
 */
public class ISTHBLB20XAC extends LBXACWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 1568107024307749233L;

    /**
     * 
     */
    public ISTHBLB20XAC() {
        super();
        this.techLevel = TechConstants.T_IS_UNOFFICIAL;
        this.name = "LB 20-X AC (THB)";
        this.setInternalName("ISTHBLBXAC20");
        this.addLookupName("IS LB 20-X AC (THB)");
        this.heat = 6;
        this.damage = 20;
        this.rackSize = 20;
        this.ammoType = AmmoType.T_AC_LBX_THB;
        this.shortRange = 4;
        this.mediumRange = 7;
        this.longRange = 12;
        this.extremeRange = 14;
        this.tonnage = 14.0f;
        this.criticals = 10;
        this.flags |= F_SPLITABLE;
        this.bv = 204;
        this.cost = 700000;
    }
}

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
 * Created on Oct 1, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.TechConstants;

/**
 * @author Andrew Hunter
 */
public class ISTHBUAC2 extends UACWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 8027434391024117813L;

    /**
     * 
     */
    public ISTHBUAC2() {
        super();
        this.techLevel = TechConstants.T_IS_UNOFFICIAL;
        this.name = "Ultra AC/2 (THB)";
        this.setInternalName("ISUltraAC2 (THB)");
        this.addLookupName("IS Ultra AC/2 (THB)");
        this.heat = 1;
        this.damage = 2;
        this.rackSize = 2;
        this.ammoType = AmmoType.T_AC_ULTRA_THB;
        this.minimumRange = 3;
        this.shortRange = 9;
        this.mediumRange = 20;
        this.longRange = 32;
        this.extremeRange = 40;
        this.tonnage = 7.0f;
        this.criticals = 3;
        this.bv = 67;
        this.cost = 150000;
    }
}

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
public class ISTHBUAC10 extends UACWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = -8651956434662071593L;

    /**
     * 
     */
    public ISTHBUAC10() {
        super();
        this.techLevel = TechConstants.T_IS_UNOFFICIAL;
        this.name = "Ultra AC/10 (THB)";
        this.setInternalName("ISUltraAC10 (THB)");
        this.addLookupName("IS Ultra AC/10 (THB)");
        this.heat = 4;
        this.damage = 10;
        this.rackSize = 10;
        this.ammoType = AmmoType.T_AC_ULTRA_THB;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 21;
        this.extremeRange = 28;
        this.tonnage = 13.0f;
        this.criticals = 7;
        this.bv = 245;
        this.cost = 400000;
    }
}

/* MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 25, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.TechConstants;

/**
 * @author Jay Lawson
 */
public class KillerWhaleWeapon extends CapitalMissileWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 8756042527483383101L;

    /**
     * 
     */
    public KillerWhaleWeapon() {
        super();
        this.techLevel = TechConstants.T_IS_LEVEL_2;
        this.name = "Killer Whale";
        this.setInternalName(this.name);
        this.addLookupName("KillerWhale");
        this.heat = 20;
        this.damage = 4;
        this.ammoType = AmmoType.T_KILLER_WHALE;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 21;
        this.extremeRange = 28;
        this.tonnage = 150.0f;
        this.bv = 769;
        this.cost = 150000;
        this.shortAV = 4;
        this.medAV = 4;
        this.longAV = 4;
        this.extAV =4;
        this.maxRange = RANGE_EXT;
    }
}

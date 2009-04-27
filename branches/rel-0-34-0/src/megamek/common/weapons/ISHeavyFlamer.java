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
 * Created on Sep 24, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.TechConstants;

/**
 * @author Andrew Hunter
 */
public class ISHeavyFlamer extends VehicleFlamerWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = -3957472644909347725L;

    /**
     * 
     */
    public ISHeavyFlamer() {
        super();
        this.techLevel = TechConstants.T_IS_ADVANCED;
        this.name = "Heavy Flamer";
        this.setInternalName(this.name);
        this.addLookupName("IS Heavy Flamer");
        this.addLookupName("ISHeavyFlamer");
        this.heat = 5;
        this.damage = 4;
        this.rackSize = 2;
        this.ammoType = AmmoType.T_HEAVY_FLAMER;
        this.shortRange = 2;
        this.mediumRange = 4;
        this.longRange = 6;
        this.extremeRange = 8;
        this.tonnage = 1.0f;
        this.criticals = 1;
        this.bv = 20;
        this.cost = 20000;
    }
}

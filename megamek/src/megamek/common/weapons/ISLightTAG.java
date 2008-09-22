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
 * Created on Sep 7, 2005
 *
 */
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks This servers both as the Fa-Shih's Light TAG and the
 *         Kage's IS Compact TAG, as the stats are the same.
 */
public class ISLightTAG extends TAGWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 3038539726901030186L;

    public ISLightTAG() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "IS Light TAG";
        this.setInternalName("ISLightTAG");
        this.tonnage = 0.5f;
        this.criticals = 1;
        this.hittable = true;
        this.spreadable = false;
        this.heat = 0;
        this.damage = 0;
        this.shortRange = 3;
        this.mediumRange = 6;
        this.longRange = 9;
        this.extremeRange = 12;
        this.bv = 0;
        this.cost = 40000;
    }
}

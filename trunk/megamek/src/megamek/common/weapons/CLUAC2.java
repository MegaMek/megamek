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
 * Created on Oct 2, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Andrew Hunter
 */
public class CLUAC2 extends UACWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 7982946203794957045L;

    /**
     * 
     */
    public CLUAC2() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "Ultra AC/2";
        this.setInternalName("CLUltraAC2");
        this.addLookupName("Clan Ultra AC/2");
        this.heat = 1;
        this.damage = 2;
        this.rackSize = 2;
        this.minimumRange = 2;
        this.shortRange = 9;
        this.mediumRange = 18;
        this.longRange = 27;
        this.extremeRange = 36;
        this.tonnage = 5.0f;
        this.criticals = 2;
        this.bv = 62;
        this.cost = 120000;
        this.shortAV = 3;
        this.medAV = 3;
        this.longAV = 3;
        this.extAV = 3;
        this.maxRange = RANGE_EXT;
        this.explosionDamage = damage;
    }
}

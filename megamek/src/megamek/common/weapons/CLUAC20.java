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
public class CLUAC20 extends UACWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 2630276807984380743L;

    /**
     * 
     */
    public CLUAC20() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "Ultra AC/20";
        this.setInternalName("CLUltraAC20");
        this.addLookupName("Clan Ultra AC/20");
        this.heat = 7;
        this.damage = 20;
        this.rackSize = 20;
        this.shortRange = 4;
        this.mediumRange = 8;
        this.longRange = 12;
        this.extremeRange = 16;
        this.tonnage = 12.0f;
        this.criticals = 8;
        this.bv = 335;
        this.cost = 480000;
        this.flags |= F_SPLITABLE;
        this.shortAV = 30;
        this.medAV = 30;
        this.maxRange = RANGE_MED;
        this.explosionDamage = damage;
   }
}

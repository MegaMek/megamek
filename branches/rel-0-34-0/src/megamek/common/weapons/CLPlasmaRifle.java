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
/*
 * Created on Sep 13, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class CLPlasmaRifle extends PlasmaMFUKWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 1758452784566087479L;

    /**
     * 
     */
    public CLPlasmaRifle() {
        super();
        this.techLevel = TechConstants.T_CLAN_UNOFFICIAL;
        this.name = "Plasma Rifle";
        this.setInternalName(this.name);
        this.addLookupName("Clan Plasma Rifle");
        this.addLookupName("CL Plasma Rifle");
        this.addLookupName("CLPlasmaRifle");
        this.addLookupName("MFUKCLPlasmaRifle");
        this.heat = 15;
        this.damage = 10;
        this.rackSize = 1;
        this.minimumRange = 2;
        this.shortRange = 6;
        this.mediumRange = 14;
        this.longRange = 22;
        this.extremeRange = 28;
        this.waterShortRange = 4;
        this.waterMediumRange = 10;
        this.waterLongRange = 15;
        this.waterExtremeRange = 20;
        this.tonnage = 6.0f;
        this.criticals = 2;
        this.bv = 400;
        this.cost = 300000;
    }
}

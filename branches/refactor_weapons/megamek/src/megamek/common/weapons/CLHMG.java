/**
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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
 * Created on Oct 20, 2004
 *
 */
package megamek.common.weapons;
import megamek.common.AmmoType;

/**
 * @author Andrew Hunter
 *
 */
public class CLHMG extends MGWeapon {

    /**
     * 
     */
    public CLHMG() {
        super();
        this.name = "Heavy Machine Gun";
        this.setInternalName("CLHeavyMG");
        this.addLookupName("Clan Heavy Machine Gun");
        this.heat = 0;
        this.damage = 3;
        this.rackSize = 3;
        this.ammoType = AmmoType.T_MG_HEAVY;
        this.minimumRange = WEAPON_NA;
        this.shortRange = 1;
        this.mediumRange = 2;
        this.longRange = 3;
        this.tonnage = 0.5f;
        this.criticals = 1;
        this.bv = 6;
    }

}

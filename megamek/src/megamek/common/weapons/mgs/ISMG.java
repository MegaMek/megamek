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
package megamek.common.weapons.mgs;

import megamek.common.WeaponType;

/**
 * @author Sebastian Brocks
 */
public class ISMG extends MGWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -4431163118750064849L;

    /**
     * 
     */
    public ISMG() {
        super();

        this.name = "Machine Gun";
        this.setInternalName(this.name);
        this.addLookupName("IS Machine Gun");
        this.addLookupName("ISMachine Gun");
        this.addLookupName("ISMG");
        this.heat = 0;
        this.damage = 2;
        this.infDamageClass = WeaponType.WEAPON_BURST_2D6;
        this.rackSize = 2;
        this.shortRange = 1;
        this.mediumRange = 2;
        this.longRange = 3;
        this.extremeRange = 4;
        this.tonnage = 0.5;
        this.criticals = 1;
        this.bv = 5;
        this.cost = 5000;
        this.shortAV = 2;
        this.maxRange = RANGE_SHORT;
        this.atClass = CLASS_POINT_DEFENSE;
        rulesRefs = "228,TM";
        flags = flags.andNot(F_PROTO_WEAPON);
        techAdvancement.setTechBase(TECH_BASE_ALL)
        	.setIntroLevel(true)
        	.setUnofficial(false)
            .setTechRating(RATING_B)
            .setAvailability(RATING_A, RATING_A, RATING_B, RATING_A)
            .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
            .setISApproximate(false, false, false,false, false)
        	.setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, 2826, DATE_NONE)
        	.setClanApproximate(false, false, false,false, false);
    }

}

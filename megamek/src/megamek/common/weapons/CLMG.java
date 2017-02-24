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
 * Created on Oct 20, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.TechConstants;
import megamek.common.TechAdvancement;
import megamek.common.WeaponType;

/**
 * @author Andrew Hunter
 */
public class CLMG extends MGWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 2557643305248678454L;

    /**
     * 
     */
    public CLMG() {
        super();

        this.name = "Machine Gun";
        this.setInternalName("CLMG");
        this.addLookupName("Clan Machine Gun");
        this.heat = 0;
        this.damage = 2;
        this.infDamageClass = WeaponType.WEAPON_BURST_2D6;
        this.rackSize = 2;
        this.shortRange = 1;
        this.mediumRange = 2;
        this.longRange = 3;
        this.extremeRange = 4;
        this.tonnage = 0.25f;
        this.criticals = 1;
        this.bv = 5;
        this.cost = 5000;
        this.shortAV = 2;
        this.maxRange = RANGE_SHORT;
        introDate = 2817;
        techLevel.put(2817, TechConstants.T_CLAN_EXPERIMENTAL);
        techLevel.put(2825, TechConstants.T_CLAN_ADVANCED);
        techLevel.put(2830, TechConstants.T_CLAN_TW);
        availRating = new int[] { RATING_X,RATING_B ,RATING_B ,RATING_A};
        techRating = RATING_C;
        rulesRefs = "228, TM";

        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_CLAN);
        techAdvancement.setClanAdvancement(2817, 2825, 2830);
        techAdvancement.setTechRating(RATING_C);
        techAdvancement.setAvailability( new int[] { RATING_X, RATING_B, RATING_B, RATING_A });
    }

}

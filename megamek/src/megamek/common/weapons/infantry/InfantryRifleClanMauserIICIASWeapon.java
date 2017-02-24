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
package megamek.common.weapons.infantry;

import megamek.common.AmmoType;
import megamek.common.TechConstants;
import megamek.common.TechAdvancement;

/**
 * @author Ben Grills
 */
public class InfantryRifleClanMauserIICIASWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryRifleClanMauserIICIASWeapon() {
        super();

        name = "Laser Rifle (Mauser IIC IAS)";
        setInternalName(name);
        addLookupName("InfantryClanMauserIICIAS");
        addLookupName("Infantry Clan Mauser IIC");
        ammoType = AmmoType.T_NA;
        tonnage = 0.0012;
        cost = 18000;
        bv = 9.82;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_ENERGY).or(F_INF_ENCUMBER);
        infantryDamage = 1.37;
        infantryRange = 3;
        introDate = 3008;
        techLevel.put(3008, TechConstants.T_CLAN_EXPERIMENTAL);
        techLevel.put(3015, TechConstants.T_CLAN_ADVANCED);
        availRating = new int[] { RATING_X,RATING_F ,RATING_E ,RATING_D};
        techRating = RATING_F;
        rulesRefs = "273, TM";
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_CLAN);
        techAdvancement.setClanAdvancement(3008, 3015, DATE_NONE);
        techAdvancement.setTechRating(RATING_F);
        techAdvancement.setAvailability( new int[] { RATING_X, RATING_F, RATING_E, RATING_D });
    }
}

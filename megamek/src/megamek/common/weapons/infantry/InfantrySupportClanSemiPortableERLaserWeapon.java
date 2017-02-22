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
import megamek.common.TechProgression;

/**
 * @author Ben Grills
 */
public class InfantrySupportClanSemiPortableERLaserWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantrySupportClanSemiPortableERLaserWeapon() {
        super();

        name = "Support Laser (ER, Semi-Portable)";
        setInternalName(name);
        addLookupName("InfantryClanERMicroLaser");
        addLookupName("InfantryClanSemiPortableERLaser");
        addLookupName("Infantry Clan Semi-Portable ER Laser");
        ammoType = AmmoType.T_NA;
        cost = 10000;
        bv = 5.02;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_ENERGY).or(F_LASER).or(F_INF_SUPPORT);
        infantryDamage = 0.70;
        infantryRange = 3;
        crew = 2;
        introDate = 3051;
        techLevel.put(3051, TechConstants.T_CLAN_EXPERIMENTAL);
        techLevel.put(3060, TechConstants.T_CLAN_ADVANCED);
        availRating = new int[] { RATING_X,RATING_X ,RATING_C ,RATING_D};
        techRating = RATING_F;
        rulesRefs = "273, TM";
        techProgression.setTechBase(TechProgression.TECH_BASE_CLAN);
        techProgression.setClanProgression(3051, 3060, DATE_NONE);
        techProgression.setTechRating(RATING_F);
        techProgression.setAvailability( new int[] { RATING_X, RATING_X, RATING_C, RATING_D });
    }
}

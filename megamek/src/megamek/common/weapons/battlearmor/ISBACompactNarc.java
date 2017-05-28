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
package megamek.common.weapons.battlearmor;

import megamek.common.TechAdvancement;
import megamek.common.weapons.NarcWeapon;

/**
 * @author Sebastian Brocks
 */
public class ISBACompactNarc extends NarcWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 6784282679924023973L;

    /**
     *
     */
    public ISBACompactNarc() {
        super();
        name = "Narc (Compact)";
        setInternalName("ISBACompactNarc");
        addLookupName("ISBACompact Narc");
        heat = 0;
        rackSize = 4;
        shortRange = 2;
        mediumRange = 4;
        longRange = 5;
        extremeRange = 8;
        bv = 16;
        flags = flags.or(F_NO_FIRES).or(F_BA_WEAPON).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        tonnage = .15;
        criticals = 1;
        cost = 15000;
		rulesRefs = "263, TM";
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_IS);
        techAdvancement.setISAdvancement(DATE_NONE, 3060, 3065);
        techAdvancement.setTechRating(RATING_E);
        techAdvancement.setAvailability( new int[] { RATING_X, RATING_F, RATING_E, RATING_D });
    }

}

//Commented out in Weapontype. Clan version is same stats as IS one. And Clan versions captures Tech progression for both.
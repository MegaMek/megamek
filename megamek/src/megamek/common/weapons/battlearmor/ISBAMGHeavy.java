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
package megamek.common.weapons.battlearmor;

import megamek.common.TechAdvancement;
import megamek.common.WeaponType;

/**
 * @author Sebastian Brocks
 */
public class ISBAMGHeavy extends BAMGWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -8064879485060186631L;

    /**
     *
     */
    public ISBAMGHeavy() {
        super();
        name = "Machine Gun (Heavy)";
        setInternalName("ISBAHeavyMachineGun");
        addLookupName("IS BA Heavy Machine Gun");
        addLookupName("ISBAHeavyMG");
        heat = 0;
        damage = 3;
        infDamageClass = WeaponType.WEAPON_BURST_2D6;
        rackSize = 3;
        shortRange = 1;
        mediumRange = 2;
        longRange = 2;
        extremeRange = 2;
        tonnage = 0.15;
        criticals = 1;
        bv = 6;
        cost = 7500;
		rulesRefs = "258, TM";
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_IS);
        techAdvancement.setISAdvancement(DATE_NONE, DATE_NONE, 3068);
        techAdvancement.setTechRating(RATING_C);
        techAdvancement.setAvailability( new int[] { RATING_X, RATING_X, RATING_C, RATING_B });
    }

}

//Commented out in Weapontype. Clan version is same stats as IS one. And Clan versions captures Tech progression for both.
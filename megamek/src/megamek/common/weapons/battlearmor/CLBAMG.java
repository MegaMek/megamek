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

import megamek.common.TechConstants;
import megamek.common.TechProgression;
import megamek.common.WeaponType;

/**
 * @author Sebastian Brocks
 */
public class CLBAMG extends BAMGWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -5021714235121936669L;

    /**
     *
     */
    public CLBAMG() {
        super();
        name = "Machine Gun (Medium)";
        setInternalName("CLBAMG");
        addLookupName("Clan BA Machine Gun");
        heat = 0;
        damage = 2;
        infDamageClass = WeaponType.WEAPON_BURST_1D6;
        rackSize = 2;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        tonnage = 0.1f;
        criticals = 1;
        bv = 5;
        cost = 5000;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_BA_WEAPON).or(F_BURST_FIRE).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        introDate = 2868;
        techLevel.put(2868, TechConstants.T_CLAN_ADVANCED);
        techLevel.put(2870, TechConstants.T_CLAN_TW);
        availRating = new int[] { RATING_X ,RATING_D ,RATING_B ,RATING_B};
        techRating = RATING_C;
        rulesRefs = "258, TM";

        techProgression.setTechBase(TechProgression.TECH_BASE_CLAN);
        techProgression.setClanProgression(DATE_NONE, 2868, 2870);
        techProgression.setTechRating(RATING_C);
        techProgression.setAvailability( new int[] { RATING_X, RATING_D, RATING_B, RATING_B });
    }

}

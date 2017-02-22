/**
- * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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

import megamek.common.TechConstants;
import megamek.common.TechProgression;
import megamek.common.WeaponType;

/**
 * @author Andrew Hunter
 */
public class CLBAFlamer extends BAFlamerWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 8782512971175525221L;

    /**
     *
     */
    public CLBAFlamer() {
        super();
        name = "Flamer [BA]";
        setInternalName("CLBAFlamer");
        addLookupName("Clan BA Flamer");
        heat = 3;
        damage = 2;
        infDamageClass = WeaponType.WEAPON_BURST_3D6;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        tonnage = 0.15f;
        criticals = 1;
        bv = 5;
        cost = 7500;
        shortAV = 2;
        maxRange = RANGE_SHORT;
        introDate = 2860;
        techLevel.put(2860, TechConstants.T_CLAN_EXPERIMENTAL);
        techLevel.put(2868, TechConstants.T_CLAN_ADVANCED);
        techLevel.put(3050, TechConstants.T_CLAN_TW);
        availRating = new int[] { RATING_X ,RATING_D ,RATING_B ,RATING_B};
        techRating = RATING_C;
        rulesRefs = "255, TO";
        techProgression.setTechBase(TechProgression.TECH_BASE_CLAN);
        techProgression.setClanProgression(2860, 2868, 3050);
        techProgression.setTechRating(RATING_C);
        techProgression.setAvailability( new int[] { RATING_X, RATING_D, RATING_B, RATING_B });
    }
}

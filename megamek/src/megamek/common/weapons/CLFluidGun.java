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
package megamek.common.weapons;

import megamek.common.TechConstants;
import megamek.common.TechProgression;

/**
 * @author beerockxs
 */
public class CLFluidGun extends FluidGunWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 5043640099544278749L;

    /**
     *
     */
    public CLFluidGun() {
        super();

        name = "Fluid Gun";
        setInternalName("Clan Fluid Gun");
        addLookupName("CLFluidGun");
        rackSize = 1;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        bv = 6;
        heat = 0;
        criticals = 2;
        tonnage = 2;
        cost = 35000;
        introDate = 2807;
        techLevel.put(2807, TechConstants.T_CLAN_TW);
        availRating = new int[] { RATING_B ,RATING_B ,RATING_B ,RATING_B};
        techRating = RATING_B;
        rulesRefs = "313, TO";
        techProgression.setTechBase(TechProgression.TECH_BASE_CLAN);
        techProgression.setClanProgression(DATE_NONE, DATE_NONE, 2807);
        techProgression.setTechRating(RATING_B);
        techProgression.setAvailability( new int[] { RATING_B, RATING_B, RATING_B, RATING_B });
    }
}

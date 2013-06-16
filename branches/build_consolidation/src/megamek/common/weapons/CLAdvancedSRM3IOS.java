/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class CLAdvancedSRM3IOS extends AdvancedSRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 6946007011975098588L;

    /**
     *
     */
    public CLAdvancedSRM3IOS() {
        super();
        techLevel.put(3071, TechConstants.T_CLAN_ADVANCED);
        name = "Advanced SRM 3 (I-OS)";
        setInternalName("CLAdvancedSRM3IOS");
        rackSize = 3;
        shortRange = 4;
        mediumRange = 8;
        longRange = 12;
        extremeRange = 16;
        bv = 9;
        tonnage -= .5f;
        flags = flags.or(F_NO_FIRES).or(F_ONESHOT);
        cost = 36000;
        techRating = RATING_B;
        availRating = new int[] { RATING_X, RATING_X, RATING_F };
        introDate = 3058;
        techLevel.put(3058, techLevel.get(3071));
        techLevel.put(3081, TechConstants.T_CLAN_TW);

    }
}

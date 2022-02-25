/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.weapons.unofficial;

import megamek.common.TechAdvancement;
import megamek.common.weapons.lrms.LRMWeapon;

/**
 * @author Sebastian Brocks
 */
public class ISLRM3 extends LRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 435741447089925036L;

    /**
     *
     */
    public ISLRM3() {
        super();
        name = "LRM 3";
        setInternalName(name);
        addLookupName("IS LRM-3");
        addLookupName("ISLRM3");
        addLookupName("IS LRM 3");
        rackSize = 3;
        minimumRange = 6;
        bv = 29;
        rulesRefs = "Unofficial";
        techAdvancement.setTechBase(TECH_BASE_IS)
        .setIntroLevel(false)
        .setUnofficial(true)
        .setTechRating(RATING_E)
        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_X)
        .setISAdvancement(DATE_NONE, DATE_NONE, 3057, DATE_NONE, DATE_NONE)
        .setISApproximate(false, false, false, false, false);
    }
}

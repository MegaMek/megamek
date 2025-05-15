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
package megamek.common.weapons.srms;

/**
 * @author Sebastian Brocks
 */
public class CLSRM3 extends SRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 1661723137877595056L;

    /**
     *
     */
    public CLSRM3() {
        super();

        name = "SRM 3";
        setInternalName("CLSRM3");
        addLookupName("Clan SRM-3");
        addLookupName("Clan SRM 3");
        rackSize = 3;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        bv = 30;
        flags = flags.or(F_NO_FIRES).andNot(F_AERO_WEAPON).andNot(F_BA_WEAPON)
        		.andNot(F_MEK_WEAPON).andNot(F_TANK_WEAPON);
        cost = 80000;
        // Per Herb all ProtoMek launcher use the ProtoMek Chassis progression.
        //But LRM Tech Base and Avail Ratings.
        rulesRefs = "231, TM";
        techAdvancement.setTechBase(TechBase.CLAN)
    	.setIntroLevel(false)
    	.setUnofficial(false)
        .setTechRating(TechRating.F)
        .setAvailability(TechRating.X, TechRating.X, TechRating.C, TechRating.C)
        .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
        .setClanApproximate(true, false, false,false, false)
        .setPrototypeFactions(Faction.CSJ)
        .setProductionFactions(Faction.CSJ);
    }
}

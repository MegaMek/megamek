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
package megamek.common.weapons.battlearmor;

import megamek.common.weapons.lrms.LRMWeapon;

/**
 * @author Sebastian Brocks
 */
public class CLBALRM5 extends LRMWeapon {
    private static final long serialVersionUID = -2860859814228145513L;

    public CLBALRM5() {
        super();
        name = "LRM 5";
        setInternalName("CLBALRM5");
        addLookupName("Clan BA LRM-5");
        addLookupName("Clan BA LRM 5");
        heat = 2;
        rackSize = 5;
        minimumRange = WEAPON_NA;
        tonnage = .175;
        criticals = 4;
        bv = 0;
        cost = 30000;
        flags = flags.or(F_NO_FIRES).or(F_BA_WEAPON).andNot(F_MEK_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        rulesRefs = "261, TM";
        techAdvancement.setTechBase(TechBase.CLAN)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.F)
                .setAvailability(TechRating.X, TechRating.X, TechRating.F, TechRating.D)
                .setClanAdvancement(3058, 3060, 3062, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.CGS)
                .setProductionFactions(Faction.CGS);
    }
}

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

import megamek.common.Mounted;
import megamek.common.alphaStrike.AlphaStrikeElement;
/**
 * @author Sebastian Brocks
 */
public class CLAdvancedSRM2 extends AdvancedSRMWeapon {
    private static final long serialVersionUID = 5607708375304559535L;

    public CLAdvancedSRM2() {
        super();
        name = "Advanced SRM 2";
        setInternalName("CLAdvancedSRM2");
        addLookupName("Clan Advanced SRM-2");
        addLookupName("Clan Advanced SRM 2");
        rackSize = 2;
        shortRange = 4;
        mediumRange = 8;
        longRange = 12;
        extremeRange = 16;
        bv = 30;
        flags = flags.or(F_NO_FIRES).or(F_BA_WEAPON).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        tonnage = .09;
        criticals = 2;
        cost = 30000;
        rulesRefs = "261, TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
                .setClanAdvancement(3052, 3056, 3066, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_CHH)
                .setProductionFactions(F_CHH);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted fcs) {
        return (range <= AlphaStrikeElement.MEDIUM_RANGE) ? 0.4 : 0;
    }
}

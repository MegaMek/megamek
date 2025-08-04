/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.weapons.missiles;

import megamek.common.Mounted;
import megamek.common.alphaStrike.AlphaStrikeElement;

/**
 * @author Sebastian Brocks
 */
public class CLATM3 extends ATMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 107949833660086492L;

    /**
     *
     */
    public CLATM3() {
        super();
        name = "ATM 3";
        setInternalName("CLATM3");
        addLookupName("Clan ATM-3");
        heat = 2;
        rackSize = 3;
        minimumRange = 4;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        tonnage = 1.5;
        criticals = 2;
        bv = 53;
        cost = 50000;
        shortAV = 4;
        medAV = 4;
        maxRange = RANGE_MED;
        rulesRefs = "229, TM";
        techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setClanAdvancement(3052, 3053, 3054, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, true, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> linked) {
        if (range <= AlphaStrikeElement.SHORT_RANGE) {
            return 0.6;
        } else if (range <= AlphaStrikeElement.MEDIUM_RANGE) {
            return 0.4;
        } else {
            return 0.2;
        }
    }

}

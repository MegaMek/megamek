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

package megamek.common.weapons.srms;

/**
 * @author Sebastian Brocks
 */
public class ISStreakSRM2 extends StreakSRMWeapon {
    private static final long serialVersionUID = 2636425754066916235L;

    public ISStreakSRM2() {
        super();

        name = "Streak SRM 2";
        setInternalName("ISStreakSRM2");
        addLookupName("IS Streak SRM-2");
        addLookupName("IS Streak SRM 2");
        heat = 2;
        rackSize = 2;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 1.5;
        criticals = 1;
        flags = flags.or(F_NO_FIRES);
        bv = 30;
        cost = 15000;
        shortAV = 4;
        maxRange = RANGE_SHORT;
        rulesRefs = "230, TM";
        flags = flags.andNot(F_PROTO_WEAPON);
        techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.D, AvailabilityValue.D)
              .setISAdvancement(2645, 2647, 2650, 2845, 3035)
              .setISApproximate(false, false, true, false, false)
              .setClanAdvancement(2645, 2647, 2650, 2845, DATE_NONE)
              .setClanApproximate(false, false, true, false, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH);
    }
}

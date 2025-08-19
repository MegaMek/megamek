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

package megamek.common.weapons.lrms.clan.streak.oneShot;

import megamek.common.SimpleTechLevel;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.weapons.lrms.StreakLRMWeapon;

/**
 * @author Sebastian Brocks
 */
public class CLStreakLRM12OS extends StreakLRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 5240577239366457930L;

    /**
     *
     */
    public CLStreakLRM12OS() {
        super();
        name = "Streak LRM 12 (OS)";
        setInternalName("CLStreakLRM12 (OS)");
        addLookupName("Clan Streak LRM-12");
        addLookupName("Clan Streak LRM 12");
        heat = 0;
        rackSize = 12;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        tonnage = 5.3;
        criticalSlots = 1;
        bv = 207;
        cost = 180000;
        flags = flags.or(F_NO_FIRES).or(F_ONE_SHOT).andNot(F_AERO_WEAPON).andNot(F_BA_WEAPON)
              .andNot(F_MEK_WEAPON).andNot(F_TANK_WEAPON).andNot(F_PROTO_WEAPON).andNot(F_ARTEMIS_COMPATIBLE);
        techAdvancement.setTechBase(TechBase.CLAN).setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(3057, 3079, 3088).setClanApproximate(false, true, false)
              .setPrototypeFactions(Faction.CCY).setProductionFactions(Faction.CJF)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
}

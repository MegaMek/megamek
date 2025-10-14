/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2007-2025 The MegaMek Team. All Rights Reserved.
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

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.weapons.lrms.StreakLRMWeapon;

/**
 * @author Sebastian Brocks
 */
public class CLStreakLRM20OS extends StreakLRMWeapon {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -7687203185594888323L;

    /**
     *
     */
    public CLStreakLRM20OS() {
        super();
        name = "Streak LRM 20 (OS)";
        setInternalName("CLOSStreakLRM20");
        addLookupName("Clan Streak LRM-20 (OS)");
        addLookupName("Clan Streak LRM 20 (OS)");
        addLookupName("CLStreakLRM20 (OS)");
        heat = 6;
        rackSize = 20;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        tonnage = 10.5;
        criticalSlots = 5;
        bv = 69;
        flags = flags.or(F_ONE_SHOT).andNot(F_PROTO_WEAPON);
        cost = 300000;
        shortAV = 20;
        medAV = 20;
        longAV = 20;
        maxRange = RANGE_LONG;
        // Per Herb all ProtoMek launcher use the ProtoMek Chassis progression.
        //But LRM Tech Base and Avail Ratings.
        rulesRefs = "327, TO";
        techAdvancement.setTechBase(TechBase.CLAN).setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(3057, 3079, 3088).setClanApproximate(false, true, false)
              .setPrototypeFactions(Faction.CCY).setProductionFactions(Faction.CJF)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
}

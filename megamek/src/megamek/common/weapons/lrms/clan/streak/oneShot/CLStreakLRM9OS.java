/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2015-2025 The MegaMek Team. All Rights Reserved.
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
public class CLStreakLRM9OS extends StreakLRMWeapon {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = 5240577239366457930L;

    /**
     *
     */
    public CLStreakLRM9OS() {
        super();
        name = "Streak LRM 9 (OS)";
        setInternalName("CLStreakLRM9OS");
        addLookupName("Clan Streak LRM-9 (OS)");
        addLookupName("Clan Streak LRM 9 (OS)");
        heat = 0;
        rackSize = 9;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        tonnage = 4.1;
        criticalSlots = 1;
        bv = 155;
        cost = 135000;
        // Per Herb all ProtoMek launcher use the ProtoMek Chassis progression.
        //But LRM Tech Base and Avail Ratings.
        rulesRefs = "139, TO:AUE";
        flags = flags.or(F_NO_FIRES).or(F_ONE_SHOT).andNot(F_AERO_WEAPON).andNot(F_BA_WEAPON)
              .andNot(F_MEK_WEAPON).andNot(F_TANK_WEAPON).andNot(F_PROTO_WEAPON).andNot(F_ARTEMIS_COMPATIBLE);
        techAdvancement.setTechBase(TechBase.CLAN).setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(3057, 3079, 3088).setClanApproximate(false, true, false)
              .setPrototypeFactions(Faction.CCY).setProductionFactions(Faction.CJF)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
}

/*
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

package megamek.common.weapons.srms.clan.streak;

import java.io.Serial;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.weapons.srms.StreakSRMWeapon;

/**
 * @author Sebastian Brocks
 */
public class CLStreakSRM6 extends StreakSRMWeapon {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -6416567366717880864L;

    /**
     *
     */
    public CLStreakSRM6() {

        this.name = "Streak SRM 6";
        this.setInternalName("CLStreakSRM6");
        this.addLookupName("Clan Streak SRM-6");
        this.addLookupName("Clan Streak SRM 6");
        this.heat = 4;
        this.rackSize = 6;
        this.shortRange = 4;
        this.mediumRange = 8;
        this.longRange = 12;
        this.extremeRange = 16;
        this.tonnage = 3.0;
        this.criticalSlots = 2;
        this.bv = 118;
        this.cost = 120000;
        this.shortAV = 12;
        this.medAV = 12;
        this.maxRange = RANGE_MED;
        rulesRefs = "230, TM";
        techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.D)
              .setClanAdvancement(2819, 2822, 2830, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, true, false, false, false)
              .setPrototypeFactions(Faction.CSA)
              .setProductionFactions(Faction.CSA);
    }
}

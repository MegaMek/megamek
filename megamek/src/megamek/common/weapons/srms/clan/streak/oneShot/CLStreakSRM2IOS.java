/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2010-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.srms.clan.streak.oneShot;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.weapons.srms.StreakSRMWeapon;

/**
 * @author Sebastian Brocks
 */
public class CLStreakSRM2IOS extends StreakSRMWeapon {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = 2219171972794110915L;

    /**
     *
     */
    public CLStreakSRM2IOS() {

        name = "Streak SRM 2 (I-OS)";
        setInternalName("CLStreakSRM2 (IOS)");
        addLookupName("Clan IOS Streak SRM-2");
        addLookupName("Clan Streak SRM 2 (IOS)");
        heat = 2;
        rackSize = 2;
        shortRange = 4;
        mediumRange = 8;
        longRange = 12;
        extremeRange = 16;
        tonnage = 0.5;
        criticalSlots = 1;
        flags = flags.or(F_NO_FIRES).or(F_ONE_SHOT).andNot(F_PROTO_WEAPON);
        bv = 8;
        cost = 12000;
        shortAV = 4;
        medAV = 4;
        maxRange = RANGE_MED;
        rulesRefs = "139, TO:AUE";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(DATE_NONE, 3058, 3081, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, true, false, false)
              .setPrototypeFactions(Faction.CNC)
              .setProductionFactions(Faction.CNC)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}

/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.prototypes.clan;

import java.io.Serial;

import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.Mounted;

/**
 * @author Sebastian Brocks
 */
public class CLStreakSRM4Prototype extends CLPrototypeStreakSRMWeapon {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -7175957508769188051L;

    /**
     *
     */
    public CLStreakSRM4Prototype() {
        super();
        name = "Prototype Streak SRM 4";
        setInternalName("CLStreakSRM4Prototype");
        shortName = "Streak SRM 4 (P)";
        heat = 3;
        rackSize = 4;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 3.0;
        criticalSlots = 2;
        bv = 59;
        cost = 60000;
        shortAV = 4;
        maxRange = RANGE_SHORT;
        rulesRefs = "91, IO:AE";
        flags = flags.or(F_PROTOTYPE).andNot(F_PROTO_WEAPON);
        techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.X, AvailabilityValue.X)
              .setClanAdvancement(2819, DATE_NONE, DATE_NONE, 2826, DATE_NONE)
              .setClanApproximate(true, false, false, true, false)
              .setPrototypeFactions(Faction.CSA);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> fcs) {
        return (range <= AlphaStrikeElement.MEDIUM_RANGE) ? 0.84 : 0;
    }
}

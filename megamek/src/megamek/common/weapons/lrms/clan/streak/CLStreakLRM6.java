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

package megamek.common.weapons.lrms.clan.streak;

import java.io.Serial;

import megamek.common.weapons.lrms.StreakLRMWeapon;

/**
 * @author Sebastian Brocks
 */
public class CLStreakLRM6 extends StreakLRMWeapon {
    @Serial
    private static final long serialVersionUID = 5240577239366457930L;

    public CLStreakLRM6() {
        super();
        name = "Streak LRM 6";
        setInternalName("CLStreakLRM6");
        addLookupName("Clan Streak LRM-6");
        addLookupName("Clan Streak LRM 6");
        heat = 0;
        rackSize = 6;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        tonnage = 2.4;
        criticalSlots = 1;
        bv = 103;
        cost = 90000;
        // Per Herb all ProtoMek launcher use the ProtoMek Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        rulesRefs = "139, TO:AUE";
        flags = flags.or(F_NO_FIRES).andNot(F_AERO_WEAPON).andNot(F_BA_WEAPON).andNot(F_MEK_WEAPON)
              .andNot(F_TANK_WEAPON).andNot(F_ARTEMIS_COMPATIBLE);
        // Tech Advancement moved to StreakLRMWeapon.java
    }
}

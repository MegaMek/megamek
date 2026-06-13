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

package megamek.common.weapons.lrms.clan.torpedo.oneShot;

import java.io.Serial;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.weapons.lrms.LRTWeapon;

/**
 * @author Sebastian Brocks
 */
public class CLLRT10IOS extends LRTWeapon {
    @Serial
    private static final long serialVersionUID = 4402946418858772353L;

    public CLLRT10IOS() {
        super();
        name = "LRT 10 (I-OS)";
        setInternalName("CLLRTorpedo10 (IOS)");
        addLookupName("Clan IOS LRT-10");
        addLookupName("Clan LRT 10 (IOS)");
        addLookupName("CLLRT10IOS");
        heat = 4;
        rackSize = 10;
        minimumRange = WEAPON_NA;
        waterShortRange = 7;
        waterMediumRange = 14;
        waterLongRange = 21;
        waterExtremeRange = 28;
        tonnage = 2.0;
        criticalSlots = 1;
        bv = 22;
        flags = flags.or(F_ONE_SHOT).andNot(F_PROTO_WEAPON);
        cost = 80000;
        rulesRefs = "139, TO:AUE";
        techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(3058, 3081, 3085, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, true, false, false, false)
              .setPrototypeFactions(Faction.CNC)
              .setProductionFactions(Faction.CNC);
    }
}

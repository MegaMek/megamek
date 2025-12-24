/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2011-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.other.clan;

import java.io.Serial;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.weapons.other.NarcWeapon;

/**
 * @author Sebastian Brocks
 */
public class CLNarcIOS extends NarcWeapon {
    @Serial
    private static final long serialVersionUID = 5553288957570246232L;

    public CLNarcIOS() {
        super();

        name = "Narc (I-OS)";
        setInternalName("CLNarcBeacon (I-OS)");
        addLookupName("Clan I-OS Narc Beacon");
        addLookupName("Clan Narc Missile Beacon (I-OS)");
        heat = 0;
        rackSize = 1;
        shortRange = 4;
        mediumRange = 8;
        longRange = 12;
        extremeRange = 16;
        tonnage = 1.5;
        criticalSlots = 1;
        flags = flags.or(F_ONE_SHOT).andNot(F_PROTO_WEAPON);
        bv = 6;
        cost = 100000;
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

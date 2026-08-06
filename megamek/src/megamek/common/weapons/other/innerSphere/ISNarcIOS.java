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

package megamek.common.weapons.other.innerSphere;

import java.io.Serial;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.weapons.other.NarcWeapon;

/**
 * @author Sebastian Brocks
 */
public class ISNarcIOS extends NarcWeapon {
    @Serial
    private static final long serialVersionUID = 8610275030183400408L;

    public ISNarcIOS() {
        super();

        name = "Narc (I-OS)";
        setInternalName("ISNarcBeacon (I-OS)");
        addLookupName("IS I-OS Narc Beacon");
        addLookupName("IS Narc Missile Beacon (I-OS)");
        heat = 0;
        rackSize = 1;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 2.5;
        criticalSlots = 2;
        flags = flags.or(F_ONE_SHOT);
        bv = 6;
        cost = 100000;
        rulesRefs = "139, TO:AUE";
        techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(3056, 3081, 3085, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.DC)
              .setProductionFactions(Faction.DC);
    }
}

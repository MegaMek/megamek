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

package megamek.common.weapons.lrms.innerSphere.torpedo.oneShot;

import java.io.Serial;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.weapons.lrms.LRTWeapon;

/**
 * @author Sebastian Brocks
 */
public class ISLRT5IOS extends LRTWeapon {
    @Serial
    private static final long serialVersionUID = -7475019239065402296L;

    public ISLRT5IOS() {
        super();

        name = "LRT 5 (I-OS)";
        setInternalName(name);
        addLookupName("IS IOS LRT-5");
        addLookupName("ISLRTorpedo5 (IOS)");
        addLookupName("IS LRT 5 (IOS)");
        heat = 2;
        rackSize = 5;
        minimumRange = 6;
        waterShortRange = 7;
        waterMediumRange = 14;
        waterLongRange = 21;
        waterExtremeRange = 28;
        tonnage = 1.5;
        criticalSlots = 1;
        bv = 9;
        flags = flags.or(F_ONE_SHOT);
        cost = 24000;
        rulesRefs = "327, TO";
        techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(3056, 3081, 3085, DATE_NONE, DATE_NONE)
              .setISApproximate(false, true, false, false, false)
              .setPrototypeFactions(Faction.DC)
              .setProductionFactions(Faction.DC);
    }
}

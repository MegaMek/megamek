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

package megamek.common.weapons.srms.innerSphere.torpedo.oneShot;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.weapons.srms.SRTWeapon;

/**
 * @author Sebastian Brocks
 */
public class ISSRT6IOS extends SRTWeapon {
    @Serial
    private static final long serialVersionUID = -1788634690534985124L;

    public ISSRT6IOS() {
        super();
        name = "SRT 6 (I-OS)";
        setInternalName("ISSRT6IOS");
        addLookupName("ISSRT6 (IOS)"); // mtf
        addLookupName("IS SRT 6 (IOS)"); // tdb
        addLookupName("IOS SRT-6"); // mep
        heat = 4;
        rackSize = 6;
        waterShortRange = 3;
        waterMediumRange = 6;
        waterLongRange = 9;
        waterExtremeRange = 12;
        tonnage = 2.5;
        criticalSlots = 2;
        bv = 12;
        flags = flags.or(F_NO_FIRES).or(F_ONE_SHOT);
        cost = 64000;
        rulesRefs = "139, TO:AUE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(DATE_NONE, 3056, 3081, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, true, false, false)
              .setPrototypeFactions(Faction.DC)
              .setProductionFactions(Faction.DC)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}

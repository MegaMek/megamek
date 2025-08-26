/*
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
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

import megamek.common.SimpleTechLevel;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.weapons.other.TSEMPWeapon;

public class ISTSEMPRepeatingCannon extends TSEMPWeapon {

    @Serial
    private static final long serialVersionUID = -4861067053206502295L;

    public ISTSEMPRepeatingCannon() {
        cost = 1200000;
        bv = 600;
        name = "TSEMP Repeating Cannon";
        setInternalName(name);
        addLookupName("ISTSEMPREPEATING");
        flags = flags.or(F_REPEATING);
        tonnage = 8;
        criticalSlots = 7;
        tankSlots = 1;
        rulesRefs = "88, IO:AE";
        techAdvancement.setTechBase(TechBase.IS).setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F)
              .setISAdvancement(3133, DATE_NONE, DATE_NONE, 3138, DATE_NONE)
              .setPrototypeFactions(Faction.RS).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
}

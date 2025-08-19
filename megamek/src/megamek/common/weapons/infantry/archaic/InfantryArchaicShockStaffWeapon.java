/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.infantry.archaic;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.weapons.infantry.InfantryWeapon;

/**
 * @author Ben Grills
 * @since Sep 7, 2005
 */
public class InfantryArchaicShockStaffWeapon extends InfantryWeapon {
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryArchaicShockStaffWeapon() {
        super();

        name = "Staff (Shock Staff)";
        setInternalName(name);
        addLookupName("InfantryShockStaff");
        addLookupName("ShockStaff");
        ammoType = AmmoType.AmmoTypeEnum.NA;
        cost = 1500;
        tonnage = 0.003;
        bv = 0.0;
        flags = flags.or(F_NO_FIRES).or(F_INF_POINT_BLANK).or(F_INF_NONPENETRATING);
        infantryDamage = 0.21;
        infantryRange = 0;
        rulesRefs = "195, AToW-C";
        techAdvancement.setTechBase(TechBase.IS).setISAdvancement(3074, 3077, 3130, DATE_NONE, DATE_NONE)
              .setISApproximate(true, true, false, false, false)
              .setClanAdvancement(3074, 3077, 3130, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, false, false, false).setPrototypeFactions(Faction.CC)
              .setProductionFactions(Faction.CC).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E);
    }
}

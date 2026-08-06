/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.infantry.prosthetic;

import java.io.Serial;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.weapons.infantry.InfantryWeapon;

/**
 * @author Dave Nawton
 * @since Sep 7, 2005
 */
public class InfantryProstheticVibroBladeWeapon extends InfantryWeapon {
    @Serial
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryProstheticVibroBladeWeapon() {
        super();

        name = "Prosthetic Vibro Blade";
        setInternalName(name);
        addLookupName("ProstheticVibroBlade");
        ammoType = AmmoType.AmmoTypeEnum.NA;
        cost = 1000;
        bv = 0.0;
        flags = flags.or(F_NO_FIRES).or(F_INF_POINT_BLANK).or(F_INF_ARCHAIC);
        infantryDamage = 0.14;
        infantryRange = 0;
        // Rating and Dates not available below is compiled from Specific
        // Weapons in IO blended with the rating for the limb itself
        rulesRefs = "78, IO:AE";
        techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.D, AvailabilityValue.D)
              .setISAdvancement(2398, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setClanAdvancement(2398, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false);
    }
}

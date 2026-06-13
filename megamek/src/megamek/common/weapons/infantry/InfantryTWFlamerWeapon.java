/*
 * Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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

/*
 * Created on Sep 7, 2005
 *
 */
package megamek.common.weapons.infantry;

import java.io.Serial;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;

/**
 * @author Sebastian Brocks
 */
public class InfantryTWFlamerWeapon extends InfantryWeapon {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -5741978934100309295L;

    public InfantryTWFlamerWeapon() {
        super();
        // Range 1, reduced damage flamer for TW Platoon support

        name = "Total Warfare Flamer";
        setInternalName(name);
        addLookupName("InfantryTWFlamer");
        addLookupName("InfantryTWPortableFlamer");
        cost = 100;
        bv = 0.36;
        tonnage = .015;
        flags = flags.or(F_DIRECT_FIRE).or(F_FLAMER).or(F_ENERGY).or(F_INF_ENCUMBER).or(F_INF_SUPPORT);
        infantryDamage = 0.35;
        infantryRange = 1;
        crew = 1;
        // This Weapon has been replaced by the Flamer Pistol. Moving to
        // Unofficial.
        techAdvancement.setTechBase(TechBase.ALL)
              .setISAdvancement(2100, 2100, 2100, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setClanAdvancement(2100, 2100, 2100, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, false, false, false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B)
              .setUnofficial(true);
    }
}

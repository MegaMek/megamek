/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2009-2025 The MegaMek Team. All Rights Reserved.
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
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.weapons.other.FluidGunWeapon;

/**
 * @author beerockxs
 */
public class CLFluidGun extends FluidGunWeapon {
    @Serial
    private static final long serialVersionUID = 5043640099544278749L;

    public CLFluidGun() {
        super();

        name = "Fluid Gun";
        setInternalName("Clan Fluid Gun");
        addLookupName("CLFluidGun");
        rackSize = 1;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        bv = 6.0;
        heat = 0;
        criticalSlots = 2;
        svSlots = 1;
        tonnage = 2;
        cost = 35000;
        rulesRefs = "125, TO:AUE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        // December 2021 - CGL requested we move this to Advanced.
        techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B)
              .setClanAdvancement(DATE_PS, DATE_PS, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, false, false, false);
    }
}

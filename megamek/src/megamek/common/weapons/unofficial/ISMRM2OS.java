/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.unofficial;

import megamek.common.weapons.missiles.MRMWeapon;

/**
 * @author Sebastian Brocks
 */
public class ISMRM2OS extends MRMWeapon {
    private static final long serialVersionUID = -8616767319138511565L;

    public ISMRM2OS() {
        super();
        name = "MRM 2 (OS)";
        setInternalName(name);
        addLookupName("ISMRM2OS");
        rackSize = 2;
        shortRange = 3;
        mediumRange = 8;
        longRange = 15;
        extremeRange = 16;
        bv = 3;
        flags = flags.or(F_ONESHOT);
        rulesRefs = "Unofficial";
        techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(true)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.X)
              .setISAdvancement(DATE_NONE, DATE_NONE, 3057, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false);
    }
}

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

package megamek.common.weapons.prototypes;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;

/**
 * @author Sebastian Brocks
 */
public class PrototypeRL15 extends PrototypeRLWeapon {
    @Serial
    private static final long serialVersionUID = 5888570332510350564L;

    public PrototypeRL15() {
        super();

        name = "Prototype Rocket Launcher 15";
        setInternalName("CLRocketLauncher15Prototype");
        addLookupName("ISPrimRL15");
        shortName = "RL/15 (P)";
        heat = 4;
        rackSize = 15;
        shortRange = 4;
        mediumRange = 9;
        longRange = 15;
        extremeRange = 22;
        tonnage = 1.0;
        criticalSlots = 2;
        bv = 18;
        cost = 30000;
        shortAV = 9;
        medAV = 9;
        maxRange = RANGE_MED;
        flags = flags.or(F_PROTOTYPE);
        rulesRefs = "67, IO:AE";
        techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.X)
              .setISAdvancement(DATE_ES, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setClanAdvancement(DATE_ES, DATE_NONE, DATE_NONE, 2823, DATE_NONE)
              .setClanApproximate(true, false, false, true, false)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
}

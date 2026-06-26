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
public class PrototypeRL20 extends PrototypeRLWeapon {
    @Serial
    private static final long serialVersionUID = -1726964787852929811L;

    public PrototypeRL20() {
        super();

        name = "Prototype Rocket Launcher 20";
        setInternalName("RocketLauncher20Prototype");
        addLookupName("CLRocketLauncher20Prototype");
        shortName = "RL/20 (P)";
        heat = 5;
        rackSize = 20;
        shortRange = 3;
        mediumRange = 7;
        longRange = 12;
        extremeRange = 18;
        tonnage = 1.5;
        criticalSlots = 3;
        bv = 19;
        cost = 45000;
        shortAV = 12;
        medAV = 12;
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

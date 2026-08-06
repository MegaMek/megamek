/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2008-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.subCapitalWeapons;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;

/**
 * @author Jay Lawson
 * @since Sep 25, 2004
 */
public class SubCapCannonWeaponLight extends SubCapCannonWeapon {
    @Serial
    private static final long serialVersionUID = 8756042527483383101L;

    public SubCapCannonWeaponLight() {
        super();
        name = "Sub-Capital Cannon (Light)";
        setInternalName(this.name);
        addLookupName("LightSCC");
        addLookupName("Light Sub-Capital Cannon");
        shortName = "Light SCC";
        sortingName = "Sub-Capital Cannon B";
        heat = 12;
        damage = 2;
        rackSize = 2;
        shortRange = 11;
        mediumRange = 22;
        longRange = 33;
        extremeRange = 44;
        tonnage = 200.0;
        bv = 379;
        cost = 330000;
        shortAV = 2;
        medAV = 2;
        longAV = 2;
        maxRange = RANGE_LONG;
        rulesRefs = "155, TO:AUE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.D)
              .setISAdvancement(DATE_NONE, 3068, 3073, DATE_NONE, DATE_NONE)
              .setISApproximate(false, true, false, false, false)
              .setClanAdvancement(DATE_NONE, 3090, 3091, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, true, false, false, false)
              .setPrototypeFactions(Faction.WB)
              .setProductionFactions(Faction.WB)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}

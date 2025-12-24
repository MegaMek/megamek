/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.autoCannons.clan;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.weapons.autoCannons.ACWeapon;

/**
 * @author Andrew Hunter
 * @since Sep 25, 2004
 */
public class CLImprovedAC5 extends ACWeapon {
    @Serial
    private static final long serialVersionUID = 8756042527483383101L;

    public CLImprovedAC5() {
        super();
        name = "Improved Autocannon/5";
        setInternalName("Improved Autocannon/5");
        addLookupName("CLIMPAC5");
        sortingName = "Improved Autocannon/05";
        heat = 1;
        damage = 5;
        rackSize = 5;
        minimumRange = 3;
        shortRange = 6;
        mediumRange = 12;
        longRange = 18;
        extremeRange = 24;
        tonnage = 7.0;
        criticalSlots = 2;
        bv = 70.0;
        cost = 125000;
        shortAV = 5;
        medAV = 5;
        maxRange = RANGE_MED;
        explosionDamage = damage;
        ammoType = AmmoType.AmmoTypeEnum.AC_IMP;
        rulesRefs = "90, IO:AE";
        techAdvancement.setTechBase(TechBase.CLAN).setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.X, AvailabilityValue.X)
              .setClanAdvancement(DATE_NONE, 2815, 2818, 2833, 3080)
              .setClanApproximate(false, true, false, false, false)
              .setProductionFactions(Faction.CLAN).setReintroductionFactions(Faction.EI)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
}

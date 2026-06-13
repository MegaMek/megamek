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
public class CLImprovedAC10 extends ACWeapon {
    @Serial
    private static final long serialVersionUID = 814114264108820161L;

    public CLImprovedAC10() {
        super();

        name = "Improved Autocannon/10";
        setInternalName("Improved Autocannon/10");
        addLookupName("CLIMPAC10");
        heat = 3;
        damage = 10;
        rackSize = 10;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        tonnage = 11.0;
        criticalSlots = 6;
        bv = 123.0;
        cost = 200000;
        shortAV = 10;
        medAV = 10;
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

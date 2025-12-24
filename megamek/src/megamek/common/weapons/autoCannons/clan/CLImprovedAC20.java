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
public class CLImprovedAC20 extends ACWeapon {
    @Serial
    private static final long serialVersionUID = 49211848611799265L;

    public CLImprovedAC20() {
        super();

        name = "Improved Autocannon/20";
        setInternalName("Improved Autocannon/20");
        addLookupName("CLIMPAC20");
        heat = 7;
        damage = 20;
        rackSize = 20;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 13.0;
        criticalSlots = 9;
        bv = 178.0;
        cost = 300000;
        explosive = true; // when firing incendiary ammo
        shortAV = 20;
        maxRange = RANGE_SHORT;
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

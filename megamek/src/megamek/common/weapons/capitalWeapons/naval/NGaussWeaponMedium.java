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

package megamek.common.weapons.capitalWeapons.naval;

import java.io.Serial;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;

/**
 * @author Jay Lawson
 * @since Sep 25, 2004
 */
public class NGaussWeaponMedium extends NGaussWeapon {
    @Serial
    private static final long serialVersionUID = 8756042527483383101L;

    public NGaussWeaponMedium() {
        super();
        name = "Naval Gauss (Medium)";
        setInternalName(this.name);
        addLookupName("MediumNGauss");
        addLookupName("CLMediumNGauss");
        addLookupName("Medium N-Gauss (Clan)");
        shortName = "Medium NGauss";
        sortingName = "Gauss Naval C";
        heat = 15;
        damage = 25;
        ammoType = AmmoType.AmmoTypeEnum.MED_NGAUSS;
        shortRange = 13;
        mediumRange = 26;
        longRange = 39;
        extremeRange = 52;
        tonnage = 5500;
        bv = 5040.0;
        cost = 30350000;
        shortAV = 25;
        medAV = 25;
        longAV = 25;
        extAV = 25;
        maxRange = RANGE_EXT;
        rulesRefs = "145, TO:AUE";
        techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.X)
              .setISAdvancement(2440, 2448, DATE_NONE, 2950, 3052)
              .setISApproximate(true, true, false, true, false)
              .setClanAdvancement(2440, 2448, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, false, false, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH)
              .setReintroductionFactions(Faction.DC);
    }
}

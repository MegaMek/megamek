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

package megamek.common.weapons.lasers.clan.small;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.weapons.lasers.clan.CLChemicalLaserWeapon;

/**
 * @author Jason Tighe
 * @since May 29, 2004
 */
public class CLChemicalLaserSmall extends CLChemicalLaserWeapon {
    @Serial
    private static final long serialVersionUID = 322396740172378519L;

    public CLChemicalLaserSmall() {
        name = "Small Chemical Laser";
        setInternalName("CLSmallChemicalLaser");
        addLookupName("CLSmallChemLaser");
        addLookupName("Small Chem Laser");
        sortingName = "Chem Laser B";
        heat = 1;
        rackSize = 3;
        damage = 3;
        minimumRange = WEAPON_NA;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        waterShortRange = 1;
        waterMediumRange = 2;
        waterLongRange = 2;
        waterExtremeRange = 2;
        tonnage = 0.5;
        criticalSlots = 1;
        bv = 7;
        cost = 10000;
        shortAV = 3;
        maxRange = RANGE_SHORT;
        atClass = CLASS_LASER;
        rulesRefs = "132, TO:AUE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
              .setClanAdvancement(DATE_NONE, 3059, 3083, DATE_NONE, DATE_NONE)
              .setPrototypeFactions(Faction.CHH)
              .setProductionFactions(Faction.CHH)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    @Override
    public boolean isAlphaStrikePointDefense() {
        return true;
    }

}

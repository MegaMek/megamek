/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2007-2025 The MegaMek Team. All Rights Reserved.
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
import megamek.common.equipment.WeaponType;
import megamek.common.weapons.lasers.PulseLaserWeapon;

/**
 * @author Sebastian Brocks
 * @since Sep 12, 2004
 */
public class CLERPulseLaserSmall extends PulseLaserWeapon {
    @Serial
    private static final long serialVersionUID = -273231806790327505L;

    public CLERPulseLaserSmall() {
        super();
        name = "ER Small Pulse Laser";
        setInternalName("CLERSmallPulseLaser");
        addLookupName("Clan ER Pulse Small Laser");
        addLookupName("Clan ER Small Pulse Laser");
        addLookupName("ClanERSmallPulseLaser");
        sortingName = "Laser Pulse ER B";
        heat = 3;
        damage = 5;
        infDamageClass = WeaponType.WEAPON_BURST_1D6;
        toHitModifier = -1;
        shortRange = 2;
        mediumRange = 4;
        longRange = 6;
        extremeRange = 8;
        waterShortRange = 1;
        waterMediumRange = 2;
        waterLongRange = 4;
        waterExtremeRange = 6;
        tonnage = 1.5;
        criticalSlots = 1;
        shortAV = 5;
        maxRange = RANGE_SHORT;
        bv = 36;
        cost = 30000;
        flags = flags.or(F_BURST_FIRE);
        rulesRefs = "132, TO:AUE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setClanAdvancement(DATE_NONE, 3057, 3082, DATE_NONE, DATE_NONE)
              .setPrototypeFactions(Faction.CWF)
              .setProductionFactions(Faction.CWF)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}

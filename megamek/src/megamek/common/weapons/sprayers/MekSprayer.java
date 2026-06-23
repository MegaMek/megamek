/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.common.weapons.sprayers;

import java.io.Serial;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.WeaponType;

/**
 * The Mek-scale Sprayer (TM pp.248-249). Mounted on IndustrialMeks, where the additional pumping
 * mechanisms make it weigh 0.5 tons rather than the 15 kg of the vehicular version.
 *
 * @author The MegaMek Team
 */
public class MekSprayer extends SprayerWeapon {
    @Serial
    private static final long serialVersionUID = 4044003339645581943L;

    public MekSprayer() {
        super();
        // CHECKSTYLE IGNORE ForbiddenWords FOR 3 LINES
        name = "Sprayer (Mech)";
        setInternalName(EquipmentTypeLookup.SPRAYER_MEK);
        addLookupName("Sprayer [Mech]");
        shortName = "Sprayer";
        sortingName = "Sprayer Mek";
        tonnage = 0.5;
        criticalSlots = 1;
        cost = 1000;
        flags = flags.or(WeaponType.F_MEK_WEAPON);
        rulesRefs = "248, TM";
        techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B)
              .setISAdvancement(2305, 2315, 2320, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setClanAdvancement(2305, 2315, 2320, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TA)
              .setProductionFactions(Faction.FS);
    }
}

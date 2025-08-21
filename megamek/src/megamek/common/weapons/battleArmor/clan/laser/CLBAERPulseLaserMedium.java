/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.battleArmor.clan.laser;

import megamek.common.SimpleTechLevel;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.weapons.lasers.PulseLaserWeapon;

/**
 * @author Sebastian Brocks
 * @since Sep 12, 2004
 */
public class CLBAERPulseLaserMedium extends PulseLaserWeapon {
    private static final long serialVersionUID = 7816191920104768204L;

    public CLBAERPulseLaserMedium() {
        super();
        name = "ER Medium Pulse Laser";
        setInternalName("BACLERMediumPulseLaser");
        addLookupName("CLBAERMediumPulseLaser");
        addLookupName("BA Clan ER Pulse Med Laser");
        addLookupName("BA Clan ER Medium Pulse Laser");
        sortingName = "Laser Pulse ER C";
        heat = 6;
        damage = 7;
        toHitModifier = -1;
        shortRange = 5;
        mediumRange = 9;
        longRange = 14;
        extremeRange = 21;
        waterShortRange = 3;
        waterMediumRange = 5;
        waterLongRange = 8;
        waterExtremeRange = 12;
        tonnage = .8;
        criticalSlots = 4;
        bv = 117;
        cost = 150000;
        flags = flags.or(F_NO_FIRES)
              .or(F_BA_WEAPON)
              .andNot(F_MEK_WEAPON)
              .andNot(F_TANK_WEAPON)
              .andNot(F_AERO_WEAPON)
              .andNot(F_PROTO_WEAPON);
        rulesRefs = "258, TM";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false).setUnofficial(false).setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setClanAdvancement(DATE_NONE, 3057, 3082, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.CWF)
              .setProductionFactions(Faction.CWF)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}

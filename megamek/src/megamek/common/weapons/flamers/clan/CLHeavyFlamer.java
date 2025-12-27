/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2011-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.flamers.clan;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.WeaponType;
import megamek.common.weapons.flamers.VehicleFlamerWeapon;

/**
 * @author Klaus Mittag (based on ISHeavyFlamer by Andrew Hunter)
 * @since Sep 23, 2011
 */
public class CLHeavyFlamer extends VehicleFlamerWeapon {
    @Serial
    private static final long serialVersionUID = -3957472644909347725L;

    public CLHeavyFlamer() {
        super();

        name = "Heavy Flamer";
        setInternalName("CLHeavyFlamer");
        addLookupName("Clan Heavy Flamer");
        addLookupName("CL Heavy Flamer");
        addLookupName("CLHeavyFlamer");
        sortingName = "Flamer D";
        heat = 5;
        damage = 4;
        infDamageClass = WeaponType.WEAPON_BURST_6D6;
        rackSize = 2;
        ammoType = AmmoType.AmmoTypeEnum.HEAVY_FLAMER;
        shortRange = 2;
        mediumRange = 3;
        longRange = 4;
        extremeRange = 6;
        tonnage = 1.5;
        criticalSlots = 1;
        bv = 15;
        shortAV = 4;
        maxRange = RANGE_SHORT;
        atClass = CLASS_POINT_DEFENSE;
        cost = 11250;
        rulesRefs = "124, TO:AUE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setClanAdvancement(DATE_NONE, 3068, 3079, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, true, false, false)
              .setPrototypeFactions(Faction.CJF)
              .setProductionFactions(Faction.CJF)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    @Override
    public int getAlphaStrikeHeatDamage(int rangeband) {
        return (rangeband == AlphaStrikeElement.RANGE_BAND_SHORT) ? 4 : 0;
    }
}

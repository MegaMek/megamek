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

package megamek.common.weapons.lasers.innerSphere.large;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.WeaponType;
import megamek.common.weapons.lasers.VariableSpeedPulseLaserWeapon;

/**
 * @author Jason Tighe
 * @since Sep 12, 2004
 */
public class ISVariableSpeedPulseLaserLarge extends VariableSpeedPulseLaserWeapon {
    @Serial
    private static final long serialVersionUID = 2676144961105838316L;

    public ISVariableSpeedPulseLaserLarge() {
        super();
        name = "Large VSP Laser";
        setInternalName("ISLargeVSPLaser");
        addLookupName("ISLVSPL");
        addLookupName("ISLargeVariableSpeedLaser");
        addLookupName("ISLargeVSP");
        sortingName = "Laser VSP D";
        heat = 10;
        damage = WeaponType.DAMAGE_VARIABLE;
        toHitModifier = -4;
        shortRange = 4;
        mediumRange = 8;
        longRange = 15;
        extremeRange = 22;
        waterShortRange = 2;
        waterMediumRange = 5;
        waterLongRange = 9;
        waterExtremeRange = 13;
        damageShort = 11;
        damageMedium = 9;
        damageLong = 7;
        tonnage = 9.0;
        criticalSlots = 4;
        bv = 123;
        cost = 465000;
        shortAV = 10;
        medAV = 7;
        maxRange = RANGE_MED;
        rulesRefs = "133, TO:AUE";
        //Nov 22 - CGL requested we move to Standard for Simple Tech Level
        techAdvancement.setTechBase(TechBase.IS).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3070, 3072, 3080).setPrototypeFactions(Faction.FW, Faction.WB)
              .setProductionFactions(Faction.FW, Faction.WB).setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    @Override
    public double getBattleForceDamage(int range) {
        if (range == AlphaStrikeElement.SHORT_RANGE) {
            return 1.265;
        } else if (range == AlphaStrikeElement.MEDIUM_RANGE) {
            return 0.863;
        } else {
            return 0;
        }
    }
}

/*
 * Copyright (C) 2011 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.other.clan;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.weapons.CLIATMWeapon;

/**
 * @author beerockxs
 */
public class CLFussilade extends CLIATMWeapon {
    @Serial
    private static final long serialVersionUID = 1237937853765733086L;

    public CLFussilade() {
        super();
        // TODO Game Rules.
        this.name = "Fusillade Launcher";
        setInternalName("Fusillade");
        addLookupName("Fussilade");
        flags = flags.or(WeaponType.F_PROTO_WEAPON).or(WeaponType.F_MISSILE)
              .or(WeaponType.F_ONE_SHOT).or(WeaponType.F_DOUBLE_ONE_SHOT)
              .andNot(F_AERO_WEAPON).andNot(F_BA_WEAPON).andNot(F_MEK_WEAPON)
              .andNot(F_TANK_WEAPON);
        rackSize = 3;
        minimumRange = 4;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        damage = 6;
        shortAV = 6;
        medAV = 6;
        longAV = 6;
        maxRange = RANGE_MED;
        cost = 100000;
        tonnage = 1.5;
        criticalSlots = 1;
        bv = 11.0;
        rulesRefs = "59, IO:AE";
        techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.X)
              .setClanAdvancement(3072, DATE_NONE, DATE_NONE, 3075, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> fcs) {
        if (range == AlphaStrikeElement.SHORT_RANGE) {
            return 0.45;
        } else if (range == AlphaStrikeElement.MEDIUM_RANGE) {
            return 0.3;
        } else {
            return 0;
        }
    }

}

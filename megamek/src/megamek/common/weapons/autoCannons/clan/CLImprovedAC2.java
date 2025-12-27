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
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.weapons.autoCannons.ACWeapon;

/**
 * @author Andrew Hunter
 * @since Sep 25, 2004
 */
public class CLImprovedAC2 extends ACWeapon {
    @Serial
    private static final long serialVersionUID = 4780847244648362671L;

    public CLImprovedAC2() {
        super();
        name = "Improved Autocannon/2";
        setInternalName("Improved Autocannon/2");
        addLookupName("CLIMPAC2");
        sortingName = "Improved Autocannon/02";
        heat = 1;
        damage = 2;
        rackSize = 2;
        minimumRange = 4;
        shortRange = 8;
        mediumRange = 16;
        longRange = 24;
        extremeRange = 32;
        tonnage = 5.0;
        criticalSlots = 1;
        bv = 37.0;
        cost = 75000;
        shortAV = 2;
        medAV = 2;
        longAV = 2;
        maxRange = RANGE_LONG;
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

    @Override
    public double getBattleForceDamage(int range, Mounted<?> ignore) {
        return range == AlphaStrikeElement.SHORT_RANGE ? 0.132 : 0.2;
    }
}

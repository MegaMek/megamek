/*
  Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.lrms.clan;

import static megamek.common.equipment.MountedHelper.isArtemisIV;
import static megamek.common.equipment.MountedHelper.isArtemisProto;
import static megamek.common.equipment.MountedHelper.isArtemisV;

import megamek.common.equipment.Mounted;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.weapons.lrms.LRMWeapon;

/**
 * @author Sebastian Brocks
 */
public class CLLRM15 extends LRMWeapon {
    private static final long serialVersionUID = 6075797537673614837L;

    public CLLRM15() {
        super();
        name = "LRM 15";
        setInternalName("CLLRM15");
        addLookupName("Clan LRM-15");
        addLookupName("Clan LRM 15");
        heat = 5;
        rackSize = 15;
        minimumRange = WEAPON_NA;
        tonnage = 3.5;
        criticals = 2;
        bv = 164;
        cost = 175000;
        shortAV = 9;
        medAV = 9;
        longAV = 9;
        maxRange = RANGE_LONG;
        rulesRefs = "229, TM";
        techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(2820, 2824, 2825, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> fcs) {
        if (isArtemisIV(fcs) || isArtemisProto(fcs)) {
            return (range <= AlphaStrikeElement.LONG_RANGE) ? 1.2 : 0;
        } else if (isArtemisV(fcs)) {
            return (range <= AlphaStrikeElement.LONG_RANGE) ? 1.26 : 0;
        } else {
            return (range <= AlphaStrikeElement.LONG_RANGE) ? 0.9 : 0;
        }
    }
}

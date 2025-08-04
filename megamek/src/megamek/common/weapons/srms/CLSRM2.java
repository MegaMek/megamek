/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.srms;

import static megamek.common.MountedHelper.isArtemisIV;
import static megamek.common.MountedHelper.isArtemisProto;
import static megamek.common.MountedHelper.isArtemisV;

import megamek.common.Mounted;
import megamek.common.alphaStrike.AlphaStrikeElement;

/**
 * @author Sebastian Brocks
 */
public class CLSRM2 extends SRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -8216939998088201265L;

    /**
     *
     */
    public CLSRM2() {
        super();
        name = "SRM 2";
        setInternalName("CLSRM2");
        addLookupName("Clan SRM-2");
        addLookupName("Clan SRM 2");
        heat = 2;
        rackSize = 2;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 0.5;
        criticals = 1;
        bv = 21;
        flags = flags.or(F_NO_FIRES);
        cost = 10000;
        shortAV = 2;
        maxRange = RANGE_SHORT;
        rulesRefs = "229, TM";
        techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(2820, 2824, 2825, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CCC)
              .setProductionFactions(Faction.CCC);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> fcs) {
        if (isArtemisIV(fcs) || isArtemisProto(fcs)) {
            return (range <= AlphaStrikeElement.MEDIUM_RANGE) ? 0.4 : 0;
        } else if (isArtemisV(fcs)) {
            return (range <= AlphaStrikeElement.MEDIUM_RANGE) ? 0.42 : 0;
        } else {
            return (range <= AlphaStrikeElement.MEDIUM_RANGE) ? 0.2 : 0;
        }
    }
}

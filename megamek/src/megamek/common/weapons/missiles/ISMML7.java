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

package megamek.common.weapons.missiles;

import static megamek.common.MountedHelper.isArtemisIV;
import static megamek.common.MountedHelper.isArtemisProto;

import megamek.common.Mounted;
import megamek.common.SimpleTechLevel;
import megamek.common.alphaStrike.AlphaStrikeElement;

/**
 * @author Sebastian Brocks
 */
public class ISMML7 extends MMLWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -2143795495566407588L;

    /**
     *
     */
    public ISMML7() {
        super();
        name = "MML 7";
        setInternalName("ISMML7");
        addLookupName("IS MML-7");
        heat = 4;
        rackSize = 7;
        tonnage = 4.5;
        criticals = 4;
        bv = 67;
        cost = 105000;
        shortAV = 4;
        medAV = 4;
        longAV = 4;
        maxRange = RANGE_LONG;
        rulesRefs = "229, TM";
        // March 2022 - CGL (Greekfire) requested MML adjustments to Tech Progression.
        techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(DATE_NONE, 3067, 3073, DATE_NONE, DATE_NONE)
              .setISApproximate(false, true, false, false, false)
              .setProductionFactions(Faction.MERC, Faction.WB)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> fcs) {
        if (range == AlphaStrikeElement.SHORT_RANGE) {
            return (isArtemisIV(fcs) || isArtemisProto(fcs)) ? 1.2 : 0.8;
        } else if (range == AlphaStrikeElement.MEDIUM_RANGE) {
            return (isArtemisIV(fcs) || isArtemisProto(fcs)) ? 0.9 : 0.6;
        } else if (range == AlphaStrikeElement.LONG_RANGE) {
            return (isArtemisIV(fcs) || isArtemisProto(fcs)) ? 0.6 : 0.4;
        } else {
            return 0;
        }
    }
}

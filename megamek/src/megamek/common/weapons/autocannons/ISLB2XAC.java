/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.autocannons;

import megamek.common.Mounted;
import megamek.common.alphaStrike.AlphaStrikeElement;

/**
 * @author Andrew Hunter
 * @since Oct 15, 2004
 */
public class ISLB2XAC extends LBXACWeapon {
    private static final long serialVersionUID = -8906248294693269096L;

    public ISLB2XAC() {
        super();
        name = "LB 2-X AC";
        setInternalName("ISLBXAC2");
        addLookupName("IS LB 2-X AC");
        sortingName = "LB 02-X AC";
        heat = 1;
        damage = 2;
        rackSize = 2;
        minimumRange = 4;
        shortRange = 9;
        mediumRange = 18;
        longRange = 27;
        extremeRange = 36;
        tonnage = 6.0;
        criticals = 4;
        bv = 42;
        cost = 150000;
        shortAV = getBaseAeroDamage();
        medAV = shortAV;
        longAV = shortAV;
        extAV = shortAV;
        maxRange = RANGE_EXT;
        rulesRefs = "207, TM";
        techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> fcs) {
        return (range <= AlphaStrikeElement.SHORT_RANGE) ? 0.069 : 0.105;
    }
}

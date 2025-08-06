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

/**
 * @author Sebastian Brocks
 */
public class ISMRM20OS extends MRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -2738014475152659505L;

    /**
     *
     */
    public ISMRM20OS() {
        super();

        name = "MRM 20 (OS)";
        setInternalName(name);
        addLookupName("OS MRM-20");
        addLookupName("ISMRM20 (OS)");
        addLookupName("IS MRM 20 (OS)");
        heat = 6;
        rackSize = 20;
        shortRange = 3;
        mediumRange = 8;
        longRange = 15;
        extremeRange = 22;
        tonnage = 7.5;
        criticals = 3;
        bv = 22;
        flags = flags.or(F_ONESHOT);
        cost = 62500;
        shortAV = 12;
        medAV = 12;
        maxRange = RANGE_MED;
        rulesRefs = "229, TM";
        techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3052, 3058, 3063, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.DC)
              .setProductionFactions(Faction.DC);
    }
}

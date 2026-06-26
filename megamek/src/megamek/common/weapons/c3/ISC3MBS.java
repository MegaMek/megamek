/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.c3;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.weapons.tag.TAGWeapon;

/**
 * @author Jason Tighe
 * @since Sep 7, 2005
 */
public class ISC3MBS extends TAGWeapon {
    @Serial
    private static final long serialVersionUID = -6402667441307181946L;

    public ISC3MBS() {
        super();
        name = "C3 Boosted System (Master)";
        shortName = "C3BS (Master)";
        setInternalName("ISC3MasterBoostedSystemUnit");
        addLookupName("IS C3 Computer Boosted");
        addLookupName("ISC3MasterComputerBoosted");
        addLookupName("C3 Master Boosted System with TAG");
        addLookupName("C3 Boosted System (C3BS) [Master]");
        tonnage = 6;
        criticalSlots = 6;
        svSlots = 6;
        tankSlots = 1;
        hittable = true;
        spreadable = false;
        cost = 3000000;
        bv = 0;
        flags = flags.or(F_C3MBS).or(F_MEK_WEAPON).or(F_TANK_WEAPON).andNot(F_AERO_WEAPON);
        heat = 0;
        damage = 0;
        shortRange = 5;
        mediumRange = 9;
        longRange = 15;
        extremeRange = 18;
        rulesRefs = "110, TO:AUE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(3073, 3100, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }

    @Override
    public boolean isC3Equipment() {
        return true;
    }
}

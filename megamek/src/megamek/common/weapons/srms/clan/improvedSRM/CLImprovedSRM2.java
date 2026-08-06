/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.srms.clan.improvedSRM;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.weapons.srms.SRMWeapon;

/**
 * @author Sebastian Brocks
 */
public class CLImprovedSRM2 extends SRMWeapon {
    @Serial
    private static final long serialVersionUID = -8486208221700793591L;

    public CLImprovedSRM2() {
        super();
        name = "Improved SRM 2";
        setInternalName(name);
        addLookupName("CLImprovedSRM2");
        heat = 2;
        rackSize = 2;
        shortRange = 4;
        mediumRange = 8;
        longRange = 12;
        extremeRange = 16;
        tonnage = 1.0;
        criticalSlots = 1;
        bv = 28;
        flags = flags.or(F_NO_FIRES);
        ammoType = AmmoType.AmmoTypeEnum.SRM_IMP;
        cost = 10000;
        this.shortAV = 3;
        this.medAV = 3;
        this.maxRange = RANGE_MED;
        rulesRefs = "90, IO:AE";
        flags = flags.andNot(F_PROTO_WEAPON);
        techAdvancement.setTechBase(TechBase.CLAN).setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.X, AvailabilityValue.X)
              .setClanAdvancement(2815, 2817, 2819, 2828, 3080)
              .setClanApproximate(true, false, false, true, false)
              .setPrototypeFactions(Faction.CCC).setProductionFactions(Faction.CCC)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }

    @Override
    public String getSortingName() {
        return "SRM IMP 2";
    }
}

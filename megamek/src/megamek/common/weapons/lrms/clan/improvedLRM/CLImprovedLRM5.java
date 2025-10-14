/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.lrms.clan.improvedLRM;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.weapons.lrms.LRMWeapon;

/**
 * @author Sebastian Brocks
 */
public class CLImprovedLRM5 extends LRMWeapon {

    @Serial
    private static final long serialVersionUID = 1922843634155860893L;

    public CLImprovedLRM5() {
        super();

        name = "Improved LRM 5";
        setInternalName(name);
        addLookupName("CLImprovedLRM5");
        addLookupName("CLImpLRM5");
        heat = 2;
        rackSize = 5;
        minimumRange = 6;
        tonnage = 1.0;
        criticalSlots = 1;
        bv = 45;
        cost = 30000;
        shortAV = 3;
        medAV = 3;
        longAV = 3;
        maxRange = RANGE_LONG;
        ammoType = AmmoType.AmmoTypeEnum.LRM_IMP;
        rulesRefs = "96, IO";
        flags = flags.andNot(F_PROTO_WEAPON);
        techAdvancement.setTechBase(TechBase.CLAN).setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.X, AvailabilityValue.X)
              .setClanAdvancement(2815, 2818, 2820, 2831, 3080)
              .setPrototypeFactions(Faction.CCY).setProductionFactions(Faction.CCY)
              .setReintroductionFactions(Faction.EI).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }

    @Override
    public String getSortingName() {
        // revert LRMWeapon's override here as the name is not just "LRM xx"
        return "Improved LRM 05";
    }
}

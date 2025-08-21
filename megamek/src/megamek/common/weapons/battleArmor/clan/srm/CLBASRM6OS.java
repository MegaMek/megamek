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

package megamek.common.weapons.battleArmor.clan.srm;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.weapons.srms.SRMWeapon;


/**
 * @author Sebastian Brocks
 */
public class CLBASRM6OS extends SRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 5184043200202465163L;

    /**
     *
     */
    public CLBASRM6OS() {
        super();
        name = "SRM 6 (OS)";
        setInternalName("CLBASRM6 (OS)");
        addLookupName("Clan BA OS SRM-6");
        addLookupName("Clan BA SRM 6 (OS)");
        addLookupName("CLBASRM6OS");
        heat = 4;
        rackSize = 6;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        bv = 12;
        tonnage = .120;
        criticalSlots = 4;
        flags = flags.or(F_NO_FIRES)
              .or(F_BA_WEAPON)
              .or(F_ONE_SHOT)
              .andNot(F_MEK_WEAPON)
              .andNot(F_TANK_WEAPON)
              .andNot(F_AERO_WEAPON)
              .andNot(F_PROTO_WEAPON);
        cost = 15000;
        shortAV = 2;
        maxRange = RANGE_SHORT;
        rulesRefs = "261, TM";
        techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.B)
              .setClanAdvancement(2865, 2868, 2870, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CWF)
              .setProductionFactions(Faction.CWF);
    }
}

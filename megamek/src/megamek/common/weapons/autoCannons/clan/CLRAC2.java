/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2007-2025 The MegaMek Team. All Rights Reserved.
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
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.weapons.autoCannons.RACWeapon;

/**
 * @author Sebastian Brocks
 * @since Oct 19, 2004
 */
public class CLRAC2 extends RACWeapon {
    @Serial
    private static final long serialVersionUID = -2134880724662962943L;

    public CLRAC2() {
        super();

        name = "Rotary AC/2";
        setInternalName("CLRotaryAC2");
        addLookupName("Clan Rotary AC/2");
        addLookupName("Clan Rotary Assault Cannon/2");
        heat = 1;
        damage = 2;
        rackSize = 2;
        minimumRange = 0;
        shortRange = 8;
        mediumRange = 17;
        longRange = 25;
        extremeRange = 37;
        tonnage = 8.0;
        criticalSlots = 4;
        bv = 161.0;
        cost = 175000;
        shortAV = 8;
        medAV = 8;
        longAV = 8;
        extAV = 8;
        maxRange = RANGE_EXT;
        rulesRefs = "98, TO:AUE";
        flags = flags.andNot(F_PROTO_WEAPON);
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(3073, DATE_NONE, 3104, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.CSF)
              .setProductionFactions(Faction.CSF)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}

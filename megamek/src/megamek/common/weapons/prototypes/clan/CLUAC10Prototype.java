/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2010-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.prototypes.clan;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;

/**
 * @author Andrew Hunter
 * @since Oct 1, 2004
 */
public class CLUAC10Prototype extends CLPrototypeUACWeapon {
    @Serial
    private static final long serialVersionUID = 6937673199956551674L;

    public CLUAC10Prototype() {
        super();
        name = "Prototype Ultra Autocannon/10";
        setInternalName("CLUltraAC10Prototype");
        shortName = "Ultra AC/10 (P)";
        heat = 4;
        damage = 10;
        rackSize = 10;
        shortRange = 6;
        mediumRange = 12;
        longRange = 18;
        extremeRange = 24;
        tonnage = 13.0;
        criticalSlots = 8;
        bv = 210;
        cost = 320000;
        shortAV = 15;
        medAV = 15;
        maxRange = RANGE_MED;
        explosionDamage = damage;
        rulesRefs = "92, IO:AE";
        flags = flags.or(F_PROTOTYPE).andNot(F_PROTO_WEAPON);
        techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.C)
              .setClanAdvancement(2820, DATE_NONE, DATE_NONE, 2825, DATE_NONE)
              .setClanApproximate(true, false, false, true, false)
              .setPrototypeFactions(Faction.CSF)
              .setProductionFactions(Faction.CSF)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
}

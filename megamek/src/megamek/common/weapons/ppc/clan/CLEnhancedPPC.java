/*
 * Copyright (c) 2004 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.ppc.clan;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.weapons.ppc.PPCWeapon;

/**
 * @author Harold "BATTLEMASTER" N.
 * @since Sep 13, 2004
 */
public class CLEnhancedPPC extends PPCWeapon {
    @Serial
    private static final long serialVersionUID = 5108976056064542099L;

    public CLEnhancedPPC() {
        super();
        this.name = "Enhanced PPC";
        this.setInternalName("CLWERPPC");
        this.addLookupName("Wolverine ER PPC");
        this.addLookupName("CLWERPPC");
        this.addLookupName("Wolverine ER PPC");
        this.addLookupName("ISEHERPPC");
        this.addLookupName("IS EH ER PPC");
        sortingName = "PPC Enhanced";
        this.heat = 15;
        this.damage = 12;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 23;
        this.extremeRange = 34;
        this.waterShortRange = 4;
        this.waterMediumRange = 10;
        this.waterLongRange = 16;
        this.waterExtremeRange = 24;
        this.tonnage = 7.0;
        this.criticalSlots = 3;
        this.bv = 329.0;
        this.cost = 300000;
        this.shortAV = 12;
        this.medAV = 12;
        this.longAV = 12;
        this.maxRange = RANGE_LONG;
        rulesRefs = "89, IO:AE";
        techAdvancement.setTechBase(TechBase.CLAN).setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.X, AvailabilityValue.E)
              .setClanAdvancement(2822, 2823, DATE_NONE, 2831, 3080)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CWV)
              .setProductionFactions(Faction.CWV)
              .setReintroductionFactions(Faction.EI)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }
}

/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2009-2025 The MegaMek Team. All Rights Reserved.
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
import megamek.common.weapons.autoCannons.ProtoMekACWeapon;

/**
 * @author Jason Tighe
 * @since Oct 2, 2004
 */
public class CLProtoMekAC8 extends ProtoMekACWeapon {
    @Serial
    private static final long serialVersionUID = 4371171653960292873L;

    public CLProtoMekAC8() {
        super();

        // CHECKSTYLE IGNORE ForbiddenWords FOR 3 LINES
        name = "ProtoMech AC/8";
        setInternalName("CLProtoMechAC8");
        addLookupName("Clan ProtoMech AC/8");
        heat = 2;
        damage = 8;
        rackSize = 8;
        minimumRange = 0;
        shortRange = 3;
        mediumRange = 7;
        longRange = 10;
        extremeRange = 15;
        tonnage = 5.5;
        criticalSlots = 4;
        bv = 66.0;
        cost = 175000;
        shortAV = 8;
        medAV = 8;
        longAV = 8;
        maxRange = RANGE_SHORT;
        explosionDamage = damage;
        rulesRefs = "98, TO:AUE";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(DATE_NONE, 3070, 3073, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, true, false, false, false)
              .setPrototypeFactions(Faction.CBS)
              .setProductionFactions(Faction.CBS)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}

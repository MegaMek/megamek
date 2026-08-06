/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2008-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.mortars.clan;

import java.io.Serial;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.weapons.mortars.MekMortarWeapon;

/**
 * @author Jason Tighe
 */
public class CLMekMortar2 extends MekMortarWeapon {
    @Serial
    private static final long serialVersionUID = 7227079222584412866L;

    public CLMekMortar2() {
        super();
        // CHECKSTYLE IGNORE ForbiddenWords FOR 2 LINES
        name = "'Mech Mortar 2";
        setInternalName("Clan Mech Mortar-2");

        addLookupName("CLMekMortar2");
        addLookupName("Clan Mek Mortar 2");
        rackSize = 2;
        minimumRange = 6;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        bv = 14.0;
        heat = 2;
        criticalSlots = 1;
        tonnage = 2.5;
        cost = 15000;
        rulesRefs = "136, TO:AUE";
        techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.E)
              .setClanAdvancement(2835, 2840, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CBR)
              .setProductionFactions(Faction.CBR);
    }
}

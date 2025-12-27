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

package megamek.common.weapons.mortars.innerSphere;

import java.io.Serial;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.weapons.mortars.MekMortarWeapon;

/**
 * @author Jason Tighe
 */
public class ISMekMortar8 extends MekMortarWeapon {
    @Serial
    private static final long serialVersionUID = -3352749710661515958L;

    public ISMekMortar8() {
        super();
        // CHECKSTYLE IGNORE ForbiddenWords FOR 2 LINES
        name = "'Mech Mortar 8";
        setInternalName("IS Mech Mortar-8");

        addLookupName("ISMekMortar8");
        addLookupName("IS Mek Mortar 8");
        rackSize = 8;
        minimumRange = 6;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        bv = 50.0;
        heat = 10;
        criticalSlots = 5;
        tonnage = 10;
        cost = 70000;
        rulesRefs = "136, TO:AUE";
        techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(2526, 2531, 3052, 2819, 3043)
              .setISApproximate(true, false, false, false, false)
              .setClanAdvancement(2526, 2531, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH)
              .setReintroductionFactions(Faction.FS, Faction.LC);
    }
}

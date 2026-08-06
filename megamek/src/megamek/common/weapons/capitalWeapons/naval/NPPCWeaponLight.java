/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.capitalWeapons.naval;

import java.io.Serial;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;

/**
 * @author Jay Lawson
 * @since Sep 25, 2004
 */
public class NPPCWeaponLight extends NPPCWeapon {
    @Serial
    private static final long serialVersionUID = 8756042527483383101L;

    public NPPCWeaponLight() {
        super();
        name = "Naval PPC (Light)";
        setInternalName(this.name);
        addLookupName("LightNPPC");
        addLookupName("Light NPPC (Clan)");
        shortName = "Light NPPC";
        sortingName = "PPC Naval B";
        heat = 105;
        damage = 7;
        shortRange = 11;
        mediumRange = 22;
        longRange = 33;
        extremeRange = 44;
        tonnage = 1400.0;
        bv = 1659.0;
        cost = 2000000;
        shortAV = 7;
        medAV = 7;
        longAV = 7;
        maxRange = RANGE_LONG;
        rulesRefs = "146, TO:AUE";
        techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.D, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
              .setISAdvancement(2350, 2356, DATE_NONE, 2950, 3052)
              .setISApproximate(true, true, false, true, false)
              .setClanAdvancement(2350, 2356, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, false, false, false)
              .setProductionFactions(Faction.TH)
              .setReintroductionFactions(Faction.DC);
    }
}

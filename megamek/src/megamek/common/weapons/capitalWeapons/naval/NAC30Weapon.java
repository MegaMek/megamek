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
public class NAC30Weapon extends NavalACWeapon {
    @Serial
    private static final long serialVersionUID = 8756042527483383101L;

    public NAC30Weapon() {
        super();
        this.name = "Naval Autocannon (NAC/30)";
        this.setInternalName(this.name);
        this.addLookupName("NAC30");
        this.shortName = "NAC/30";
        this.heat = 100;
        this.damage = 30;
        this.rackSize = 30;
        this.shortRange = 9;
        this.mediumRange = 18;
        this.longRange = 27;
        this.extremeRange = 36;
        this.tonnage = 3500.0;
        this.bv = 5688.0;
        this.cost = 10500000;
        this.shortAV = 30;
        this.medAV = 30;
        this.longAV = 30;
        this.maxRange = RANGE_LONG;
        rulesRefs = "143, TO:AUE";
        techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
              .setISAdvancement(DATE_ES, 2200, DATE_NONE, 2950, 3051)
              .setISApproximate(false, true, false, true, false)
              .setClanAdvancement(DATE_ES, 2200, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, true, false, false, false)
              .setProductionFactions(Faction.TA)
              .setReintroductionFactions(Faction.FS, Faction.LC);
    }
}

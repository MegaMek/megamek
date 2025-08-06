/*
  Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.unofficial;

import megamek.common.weapons.autocannons.ACWeapon;

/**
 * @author BATTLEMASTER IIC
 * @since Sep 25, 2004
 */
public class ISAC15 extends ACWeapon {
    private static final long serialVersionUID = 814114264108820161L;

    public ISAC15() {
        super();
        name = "AC/15";
        setInternalName("Autocannon/15");
        addLookupName("IS Auto Cannon/15");
        addLookupName("Auto Cannon/15");
        addLookupName("AutoCannon/15");
        addLookupName("AC/15");
        addLookupName("ISAC15");
        addLookupName("IS Autocannon/15");
        heat = 5;
        damage = 15;
        rackSize = 15;
        shortRange = 4;
        mediumRange = 8;
        longRange = 12;
        extremeRange = 16;
        tonnage = 13.0;
        criticals = 8;
        bv = 178;
        cost = 250000;
        shortAV = 15;
        medAV = 15;
        maxRange = RANGE_MED;
        explosionDamage = damage;
        // This being an official Weapon I'm using the AC20 information
        rulesRefs = "Unofficial";
        techAdvancement.setTechBase(TechBase.ALL)
              .setUnofficial(true)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.D, AvailabilityValue.E, AvailabilityValue.D, AvailabilityValue.D)
              .setISAdvancement(2488, 2500, 2502, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setClanAdvancement(2488, 2500, 2502, 2850, DATE_NONE)
              .setClanApproximate(false, false, false, true, false)
              .setPrototypeFactions(Faction.LC)
              .setProductionFactions(Faction.LC);
    }
}

/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.autocannons;

import megamek.common.SimpleTechLevel;

/**
 * @author Andrew Hunter
 * @since Oct 19, 2004
 */
public class ISRAC5 extends RACWeapon {
    private static final long serialVersionUID = 1212976417295270466L;

    public ISRAC5() {
        super();
        this.name = "Rotary AC/5";
        this.setInternalName("ISRotaryAC5");
        this.addLookupName("IS Rotary AC/5");
        this.addLookupName("ISRAC5");
        this.heat = 1;
        this.damage = 5;
        this.rackSize = 5;
        this.shortRange = 5;
        this.mediumRange = 10;
        this.longRange = 15;
        this.extremeRange = 20;
        this.tonnage = 10.0;
        this.criticals = 6;
        this.bv = 247;
        this.cost = 275000;
        this.shortAV = 20;
        this.medAV = 20;
        this.maxRange = RANGE_MED;
        this.explosionDamage = damage;
        rulesRefs = "207, TM";
        techAdvancement.setTechBase(TechBase.IS)
              .setStaticTechLevel(SimpleTechLevel.STANDARD)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3060, 3062, 3071, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS);
    }
}

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

package megamek.common.weapons.autocannons.innerSphere;

import megamek.common.SimpleTechLevel;
import megamek.common.weapons.autocannons.RACWeapon;

/**
 * @author Andrew Hunter
 * @since Oct 19, 2004
 */
public class ISRAC2 extends RACWeapon {
    private static final long serialVersionUID = 7256023025545151994L;

    public ISRAC2() {
        super();
        this.name = "Rotary AC/2";
        this.setInternalName("ISRotaryAC2");
        this.addLookupName("IS Rotary AC/2");
        this.addLookupName("ISRAC2");
        this.heat = 1;
        this.damage = 2;
        this.rackSize = 2;
        this.shortRange = 6;
        this.mediumRange = 12;
        this.longRange = 18;
        this.extremeRange = 24;
        this.tonnage = 8.0;
        this.criticals = 3;
        this.bv = 118;
        this.cost = 175000;
        this.shortAV = 8;
        this.medAV = 8;
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

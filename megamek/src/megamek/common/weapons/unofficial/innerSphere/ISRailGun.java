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

package megamek.common.weapons.unofficial.innerSphere;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.weapons.gaussrifles.GaussWeapon;

/**
 * @author Andrew Hunter
 * @since Sep 25, 2004
 */
public class ISRailGun extends GaussWeapon {
    private static final long serialVersionUID = 8879671694066711976L;

    public ISRailGun() {
        super();
        this.name = "Rail Gun";
        this.setInternalName("ISRailGun");
        this.addLookupName("IS Rail Gun");
        this.heat = 1;
        this.damage = 22;
        this.ammoType = AmmoType.AmmoTypeEnum.RAIL_GUN;
        this.minimumRange = 1;
        this.shortRange = 6;
        this.mediumRange = 13;
        this.longRange = 19;
        this.extremeRange = 26;
        this.tonnage = 18.0;
        this.criticalSlots = 9;
        this.bv = 411;
        this.cost = 300000;
        this.explosionDamage = 20;
        // This appears to be like the Heavy Gauss using those stats.
        rulesRefs = "Unofficial";
        techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(true)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3051, 3061, 3067, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FW)
              .setProductionFactions(Faction.FC);
    }
}

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

package megamek.common.weapons.battleArmor.innerSphere;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.weapons.tag.TAGWeapon;

/**
 * This serves both as the Fa-Shih's Light TAG and the Kage's IS Compact TAG, as the stats are the same. Commented out
 * in WeaponType. Clan version is same stats as IS one. And Clan versions captures Tech progression for both.
 *
 * @author Sebastian Brocks
 * @since Sep 7, 2005
 */
public class ISBALightTAG extends TAGWeapon {
    private static final long serialVersionUID = 3038539726901030186L;

    public ISBALightTAG() {
        super();
        this.name = "TAG (Light)";
        setInternalName("ISBALightTAG");
        this.addLookupName("IS BA Light TAG");
        this.tonnage = 0.035;
        this.criticalSlots = 1;
        this.hittable = true;
        this.spreadable = false;
        this.heat = 0;
        this.damage = 0;
        this.shortRange = 3;
        this.mediumRange = 6;
        this.longRange = 9;
        this.extremeRange = 12;
        this.bv = 0;
        this.cost = 40000;
        flags = flags.or(F_NO_FIRES).or(F_BA_WEAPON).andNot(F_MEK_WEAPON).andNot(F_TANK_WEAPON)
              .andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        rulesRefs = "270, TM";
        techAdvancement.setTechBase(TechBase.IS);
        techAdvancement.setISAdvancement(3046, 3053, 3057);
        techAdvancement.setTechRating(TechRating.E);
        techAdvancement.setAvailability(AvailabilityValue.X,
              AvailabilityValue.X,
              AvailabilityValue.F,
              AvailabilityValue.E);
    }
}

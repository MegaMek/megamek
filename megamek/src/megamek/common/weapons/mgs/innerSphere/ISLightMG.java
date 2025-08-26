/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2007-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.mgs.innerSphere;

import java.io.Serial;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.WeaponType;
import megamek.common.weapons.mgs.MGWeapon;

/**
 * @author Sebastian Brocks
 * @since Oct 20, 2004
 */
public class ISLightMG extends MGWeapon {
    @Serial
    private static final long serialVersionUID = 8148848145274790948L;

    public ISLightMG() {
        super();
        name = "Light Machine Gun";
        setInternalName(this.name);
        addLookupName("IS Light Machine Gun");
        addLookupName("ISLightMG");
        sortingName = "MG B";
        ammoType = AmmoType.AmmoTypeEnum.MG_LIGHT;
        heat = 0;
        damage = 1;
        infDamageClass = WeaponType.WEAPON_BURST_1D6;
        rackSize = 1;
        shortRange = 2;
        mediumRange = 4;
        longRange = 6;
        extremeRange = 8;
        tonnage = 0.5;
        criticalSlots = 1;
        bv = 5;
        cost = 5000;
        shortAV = 1;
        maxRange = RANGE_SHORT;
        rulesRefs = "228, TM";
        techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.B)
              .setISAdvancement(3064, 3068, 3070, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CC)
              .setProductionFactions(Faction.CC);
    }
}

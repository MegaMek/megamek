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

import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.weapons.autocannons.NailRivetGunWeapon;

/**
 * @author Sebastian Brocks
 */
public class ISNailandRivetGun extends NailRivetGunWeapon {
    private static final long serialVersionUID = -5198228513368748633L;

    public ISNailandRivetGun() {
        super();
        name = "Nail/Rivet Gun";
        setInternalName(EquipmentTypeLookup.NAIL_RIVET_GUN);
        addLookupName("ISNailRivet Gun");
        addLookupName("ISNail Gun");
        addLookupName("Nail/Rivet Gun");
        addLookupName("CLNailRivet Gun");
        addLookupName("CLNail/Rivet Gun");
        addLookupName("CLNail Gun");
        addLookupName("ISRivet Gun");
        addLookupName("CLRivet Gun");
        addLookupName("CLNailRivetGun");
        addLookupName("Nail Gun");
        rulesRefs = "246, TM";
        techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C)
              .setISAdvancement(2309, 2310, 2312, DATE_NONE, DATE_NONE)
              .setISApproximate(true, true, false, false, false)
              .setClanAdvancement(2309, 2310, 2312, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, false, false, false)
              .setPrototypeFactions(Faction.FW)
              .setProductionFactions(Faction.FW);
    }
}


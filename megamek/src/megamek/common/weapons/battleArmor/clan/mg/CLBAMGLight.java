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

package megamek.common.weapons.battleArmor.clan.mg;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.WeaponType;
import megamek.common.weapons.battleArmor.BAMGWeapon;

/**
 * @author Sebastian Brocks
 * @since Oct 20, 2004
 */
public class CLBAMGLight extends BAMGWeapon {
    private static final long serialVersionUID = 1810341654439496432L;

    public CLBAMGLight() {
        super();
        name = "Machine Gun (Light)";
        setInternalName("CLBALightMG");
        addLookupName("Clan BA Light Machine Gun");
        addLookupName("ISBALightMachineGun");
        addLookupName("IS BA Light Machine Gun");
        addLookupName("ISBALightMG");
        sortingName = "MG B";
        heat = 0;
        damage = 1;
        infDamageClass = WeaponType.WEAPON_BURST_HALF_D6;
        rackSize = 1;
        shortRange = 2;
        mediumRange = 4;
        longRange = 6;
        extremeRange = 8;
        tonnage = 0.075;
        criticalSlots = 1;
        bv = 5;
        cost = 5000;
        rulesRefs = "258, TM";
        techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.B)
              .setISAdvancement(DATE_NONE, DATE_NONE, 3068, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setClanAdvancement(3055, 3060, 3068, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
    }
}

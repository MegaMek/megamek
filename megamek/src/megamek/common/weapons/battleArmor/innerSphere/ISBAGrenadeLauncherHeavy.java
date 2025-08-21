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
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.WeaponType;
import megamek.common.weapons.Weapon;

/**
 * Commented out in WeaponType. Clan version is same stats as IS one. And IS versions captures Tech progression for
 * both.
 *
 * @author Andrew Hunter
 * @since Sep 24, 2004
 */
public class ISBAGrenadeLauncherHeavy extends Weapon {
    private static final long serialVersionUID = -5514157095037913844L;

    public ISBAGrenadeLauncherHeavy() {
        super();
        name = "Grenade Launcher(Heavy)";
        setInternalName("ISBAHeavyGrenadeLauncher");
        addLookupName("BA Heavy Grenade Launcher");
        addLookupName("ISBAAutoGL");
        addLookupName("ISBAHeavyGL");
        //Per TM Errata the original Grenade Launcher becomes the Heavy. Lookups below to keep unit files consistent.
        addLookupName("ISBAGrenadeLauncher");
        addLookupName("IS BA Grenade Launcher");
        sortingName = "Grenade Launcher D";
        heat = 0;
        damage = 1;
        infDamageClass = WeaponType.WEAPON_BURST_1D6;
        ammoType = AmmoType.AmmoTypeEnum.NA;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        tonnage = 0.1;
        criticalSlots = 1;
        bv = 2;
        cost = 4500;
        flags = flags.or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_BURST_FIRE).or(F_BA_WEAPON)
              .andNot(F_MEK_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        rulesRefs = "256, TM";
        techAdvancement.setTechBase(TechBase.IS);
        techAdvancement.setISAdvancement(DATE_NONE, DATE_NONE, 3050);
        techAdvancement.setTechRating(TechRating.C);
        techAdvancement.setAvailability(AvailabilityValue.X,
              AvailabilityValue.D,
              AvailabilityValue.D,
              AvailabilityValue.C);
    }

}

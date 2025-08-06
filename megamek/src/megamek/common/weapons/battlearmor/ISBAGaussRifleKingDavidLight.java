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

package megamek.common.weapons.battlearmor;

import megamek.common.AmmoType;
import megamek.common.WeaponType;
import megamek.common.weapons.Weapon;

/**
 * @author Sebastian Brocks
 * @since Sep 24, 2004
 */
public class ISBAGaussRifleKingDavidLight extends Weapon {
    private static final long serialVersionUID = -3358799424901447503L;

    public ISBAGaussRifleKingDavidLight() {
        super();
        name = "Gauss Rifle [King David]";
        setInternalName("ISBAKingDavidLightGaussRifle");
        addLookupName("IS BA King David Light Gauss Rifle");
        damage = 1;
        baDamageClass = WeaponType.WEAPON_BURST_1D6;
        ammoType = AmmoType.AmmoTypeEnum.NA;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        bv = 7;
        tonnage = 0.275;
        criticals = 2;
        cost = 30000;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC)
              .or(F_BA_WEAPON).andNot(F_MEK_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        rulesRefs = "255, TM";
        techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3058, 3063, 3065, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FW)
              .setProductionFactions(Faction.FW);
    }
}

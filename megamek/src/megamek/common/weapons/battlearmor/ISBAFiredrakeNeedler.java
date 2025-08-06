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
public class ISBAFiredrakeNeedler extends Weapon {
    private static final long serialVersionUID = -8852176757815947141L;

    public ISBAFiredrakeNeedler() {
        super();
        name = "Needler (Firedrake)";
        setInternalName("ISBAFireDrakeNeedler");
        addLookupName("ISBAFiredrakeIncendiaryNeedler");
        damage = 1;
        infDamageClass = WeaponType.WEAPON_BURST_3D6;
        ammoType = AmmoType.AmmoTypeEnum.NA;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        bv = 2;
        cost = 1500;
        tonnage = 0.050;
        criticals = 1;
        flags = flags.or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_INCENDIARY_NEEDLES).or(F_BURST_FIRE)
              .or(F_BA_WEAPON).andNot(F_MEK_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON)
              .andNot(F_PROTO_WEAPON);
        rulesRefs = "266, TM";
        techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.B)
              .setISAdvancement(3058, 3060, 3068, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.LC)
              .setProductionFactions(Faction.LC);
    }
}

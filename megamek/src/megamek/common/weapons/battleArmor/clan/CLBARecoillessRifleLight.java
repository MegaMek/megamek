/*
 * Copyright (c) 2004-2005 - Ben Mazur (bmazur@sev.org).
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

package megamek.common.weapons.battleArmor.clan;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.WeaponType;
import megamek.common.weapons.Weapon;

/**
 * @author Andrew Hunter
 * @since Sep 24, 2004
 */
public class CLBARecoillessRifleLight extends Weapon {
    private static final long serialVersionUID = 1463215714799059271L;

    public CLBARecoillessRifleLight() {
        super();
        name = "BA Recoilless Rifle (Light)";
        setInternalName("CLBALight Recoilless Rifle");
        addLookupName("CLBALight Recoilless Rifle");
        addLookupName("CLBALightRecoillessRifle");
        addLookupName("ISBALightRecoillessRifle");
        addLookupName("ISLight Recoilless Rifle");
        addLookupName("ISBALight Recoilless Rifle");
        sortingName = "Recoilless Rifle B";
        shortName = "Light Recoiless Rifle";
        damage = 2;
        infDamageClass = WeaponType.WEAPON_BURST_1D6;
        ammoType = AmmoType.AmmoTypeEnum.NA;
        shortRange = 2;
        mediumRange = 4;
        longRange = 6;
        extremeRange = 8;
        bv = 12;
        tonnage = 0.175;
        criticalSlots = 2;
        flags = flags.or(F_NO_FIRES)
              .or(F_DIRECT_FIRE)
              .or(F_BALLISTIC)
              .or(F_BA_WEAPON)
              .or(F_BURST_FIRE)
              .andNot(F_MEK_WEAPON)
              .andNot(F_TANK_WEAPON)
              .andNot(F_AERO_WEAPON)
              .andNot(F_PROTO_WEAPON);
        cost = 1000;
        rulesRefs = "268, TM";
        techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setISAdvancement(3052, 3054, 3056, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setClanAdvancement(DATE_NONE, DATE_NONE, 3062, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, true, false, false)
              .setPrototypeFactions(Faction.FS, Faction.LC)
              .setProductionFactions(Faction.FS, Faction.LC);
    }
}

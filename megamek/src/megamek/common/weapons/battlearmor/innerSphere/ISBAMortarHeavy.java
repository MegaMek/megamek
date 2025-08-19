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

package megamek.common.weapons.battlearmor.innerSphere;

import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.weapons.Weapon;

/**
 * Commented out in WeaponType. Clan version is same stats as IS one. And Clan versions captures Tech progression for
 * both.
 *
 * @author Sebastian Brocks
 * @since Sep 24, 2004
 */
public class ISBAMortarHeavy extends Weapon {
    private static final long serialVersionUID = 7081695747408312441L;

    public ISBAMortarHeavy() {
        super();
        name = "Heavy Mortar";
        setInternalName("ISBAHeavyMortar");
        addLookupName("IS BA Heavy Mortar");
        sortingName = "Mortar D";
        damage = 3;
        infDamageClass = WeaponType.WEAPON_BURST_1D6;
        ammoType = AmmoType.AmmoTypeEnum.NA;
        minimumRange = 2;
        shortRange = 2;
        mediumRange = 4;
        longRange = 6;
        extremeRange = 8;
        bv = 17;
        cost = 7500;
        tonnage = 0.4;
        criticalSlots = 2;
        flags = flags.or(F_BALLISTIC).or(F_BURST_FIRE).or(F_BA_WEAPON)
              .andNot(F_MEK_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        rulesRefs = "263, TM";

        techAdvancement.setTechBase(TechBase.IS);
        techAdvancement.setISAdvancement(3049, 3057, 3063);
        techAdvancement.setTechRating(TechRating.B);
        techAdvancement.setAvailability(AvailabilityValue.X,
              AvailabilityValue.X,
              AvailabilityValue.C,
              AvailabilityValue.C);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> linked) {
        if (range <= AlphaStrikeElement.SHORT_RANGE) {
            return 0.249;
        } else if (range <= AlphaStrikeElement.MEDIUM_RANGE) {
            return 0.3;
        } else {
            return 0;
        }
    }

    @Override
    public boolean isAlphaStrikeIndirectFire() {
        return true;
    }
}

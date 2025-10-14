/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.missiles.thuunderbolt;

import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;

public abstract class Thunderbolt20Weapon extends ThunderboltWeapon {

    public Thunderbolt20Weapon() {
        super();
        ammoType = AmmoType.AmmoTypeEnum.TBOLT_20;
        heat = 8;
        shortAV = 20;
        medAV = 20;
        criticalSlots = 5;
        missileArmor = 20;
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> fcs) {
        if (range == AlphaStrikeElement.SHORT_RANGE) {
            return 1.16;
        } else if (range == AlphaStrikeElement.MEDIUM_RANGE) {
            return 2;
        } else if (range == AlphaStrikeElement.LONG_RANGE) {
            return 2;
        } else {
            return 0;
        }
    }

    @Override
    public String getSortingName() {
        String oneShotTag = hasFlag(F_ONE_SHOT) ? "OS" : "";
        if (name.contains("I-OS")) {
            oneShotTag = "XIOS";
        }
        return "Thunderbolt-" + oneShotTag + "20";
    }
}

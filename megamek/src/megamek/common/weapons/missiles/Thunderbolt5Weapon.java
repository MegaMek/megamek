/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.weapons.missiles;

import megamek.common.AmmoType;
import megamek.common.Mounted;
import megamek.common.SimpleTechLevel;
import megamek.common.alphaStrike.AlphaStrikeElement;

public abstract class Thunderbolt5Weapon extends ThunderBoltWeapon {

    public Thunderbolt5Weapon() {
        super();
        ammoType = AmmoType.T_TBOLT_5;
        heat = 3;
        shortAV = 5;
        medAV = 5;
        criticals = 1;
        missileArmor = 5;
    }

    @Override
    public double getBattleForceDamage(int range, Mounted fcs) {
        if (range == AlphaStrikeElement.SHORT_RANGE) {
            return 0.29;
        } else if (range == AlphaStrikeElement.MEDIUM_RANGE) {
            return 0.5;
        } else if (range == AlphaStrikeElement.LONG_RANGE) {
            return 0.5;
        } else {
            return 0;
        }
    }

    @Override
    public String getSortingName() {
        String oneShotTag = hasFlag(F_ONESHOT) ? "OS" : "";
        if (name.contains("I-OS")) {
            oneShotTag = "XIOS";
        }
        return "Thunderbolt-" + oneShotTag + "05";
    }
}
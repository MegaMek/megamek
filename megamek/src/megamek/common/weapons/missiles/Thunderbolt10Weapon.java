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

public abstract class Thunderbolt10Weapon extends ThunderBoltWeapon {

    public Thunderbolt10Weapon() {
        super();
        ammoType = AmmoType.T_TBOLT_10;
        heat = 5;
        shortAV = 10;
        medAV = 10;
        criticals = 2;
        missileArmor = 10;
    }

    @Override
    public double getBattleForceDamage(int range, Mounted fcs) {
        if (range == AlphaStrikeElement.SHORT_RANGE) {
            return 0.58;
        } else if (range <= AlphaStrikeElement.LONG_RANGE) {
            return 1;
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
        return "Thunderbolt-" + oneShotTag + "10";
    }
}

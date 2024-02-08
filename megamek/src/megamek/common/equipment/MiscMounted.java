/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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

package megamek.common.equipment;

import megamek.common.Entity;
import megamek.common.MiscType;
import megamek.common.Mounted;

public class MiscMounted extends Mounted<MiscType> {
    public MiscMounted(Entity entity, MiscType type) {
        super(entity, type);

        if (type.hasFlag(MiscType.F_MINE)) {
            setMineType(MINE_CONVENTIONAL);
            // Used to keep track of the # of mines
            setShotsLeft(1);
        }
        if (type.hasFlag(MiscType.F_VEHICLE_MINE_DISPENSER)) {
            setMineType(MINE_CONVENTIONAL);
            // Used to keep track of the # of mines
            setShotsLeft(2);
        }
        if (type.hasFlag(MiscType.F_SENSOR_DISPENSER)) {
            setShotsLeft(type.hasFlag(MiscType.F_BA_EQUIPMENT) ? 6 : 30);
        }
        if (((type.isShield() || type.hasFlag(MiscType.F_MODULAR_ARMOR)))) {
            baseDamageAbsorptionRate = type.getBaseDamageAbsorptionRate();
            baseDamageCapacity = type.getBaseDamageCapacity();
        }
        if (type.hasFlag(MiscType.F_MINESWEEPER)) {
            setArmorValue(30);
        }
    }
}

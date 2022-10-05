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
package megamek.common.battlevalue;

import megamek.common.Entity;
import megamek.common.Mounted;

public class GunEmplacementBVCalculator extends BVCalculator {

    GunEmplacementBVCalculator(Entity entity) {
        super(entity);
    }

    @Override
    protected double tmmFactor(int tmmRunning, int tmmJumping, int tmmUmu) {
        return 0.5;
    }

    @Override
    protected void processStructure() { }

    @Override
    protected int offensiveSpeedFactorMP() {
        return 0;
    }

    @Override
    protected String equipmentDescriptor(Mounted mounted) {
        return mounted.getType().getShortName();
    }
}
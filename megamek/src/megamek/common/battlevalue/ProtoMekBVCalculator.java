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

import megamek.common.*;

public class ProtoMekBVCalculator extends BVCalculator {

    Protomech protoMek = (Protomech) entity;

    ProtoMekBVCalculator(Entity entity) {
        super(entity);
    }

    @Override
    protected double getAmmoBV(Mounted ammo) {
        return ((AmmoType) ammo.getType()).getKgPerShotBV(ammo.getUsableShotsLeft());
    }

    @Override
    protected int getRunningTMM() {
        return super.getRunningTMM() + (protoMek.isGlider() ? 1 : 0);
    }

    @Override
    protected double tmmFactor(int tmmRunning, int tmmJumping, int tmmUmu) {
        return super.tmmFactor(tmmRunning, tmmJumping, tmmUmu) + 0.1;
    }
}
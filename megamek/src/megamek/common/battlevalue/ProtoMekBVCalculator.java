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

import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.Mounted;
import megamek.common.Protomech;

public class ProtoMekBVCalculator extends BVCalculator {

    ProtoMekBVCalculator(Entity entity) {
        super(entity);
    }

    @Override
    protected double getAmmoBV(Mounted ammo) {
        return ((AmmoType) ammo.getType()).getProtoBV(ammo.getUsableShotsLeft());
    }

    @Override
    protected int getRunningTMM() {
        int tmmRunning = super.getRunningTMM();
        if (((Protomech) entity).isGlider()) {
            tmmRunning++;
        }
        return tmmRunning;
    }

    @Override
    protected double tmmFactor(int tmmRunning, int tmmJumping, int tmmUmu) {
        return super.tmmFactor(tmmRunning, tmmJumping, tmmUmu) + 0.1;
    }

    @Override
    protected int speedFactorMP() {
        Protomech protoMek = (Protomech) entity;
        int mp = protoMek.getRunMPwithoutMyomerBooster(false, true, true);
        mp += (int) Math.round(Math.max(jumpMP, umuMP) / 2.0);
        if (protoMek.hasMyomerBooster()) {
            mp++;
        }
        return mp;
    }
}
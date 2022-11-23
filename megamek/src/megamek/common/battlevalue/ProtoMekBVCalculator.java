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

    /** Calculates the walk MP with the rules given for setRunMP. */
    private int walkMP() {
        int mp = protoMek.isEngineHit() ? 0 : protoMek.getOriginalWalkMP();
        if (protoMek.isGlider()) {
            // Torso crits reduce glider mp as jump
            int torsoCrits = protoMek.getCritsHit(Protomech.LOC_TORSO);
            if (torsoCrits == 1) {
                mp--;
            } else if (torsoCrits == 2) {
                mp /= 2;
            }
            // Near misses damage the wings/flight systems, which reduce MP by one per hit.
            mp -= protoMek.getWingHits();
        } else {
            int legCrits = protoMek.getCritsHit(Protomech.LOC_LEG);
            if (legCrits == 1) {
                mp--;
            } else if (legCrits == 2) {
                mp /= 2;
            } else if (legCrits == 3) {
                mp = 0;
            }
        }
        return Math.max(mp, 0);
    }

    @Override
    protected void setRunMP() {
        // TM p306: The myomer booster is treated differently for defensive and offensive speeds
        // In accordance with Mordel's calculation, I'm not implementing this
        if (protoMek.hasMyomerBooster()) {
            runMP = walkMP() * 2;
        } else {
            runMP = (int) Math.ceil(walkMP() * 1.5);
        }
    }

    @Override
    protected void setJumpMP() {
        jumpMP = protoMek.getOriginalJumpMP();
        int torsoCrits = protoMek.getCritsHit(Protomech.LOC_TORSO);
        if (torsoCrits == 1) {
            jumpMP = Math.max(jumpMP - 1, 0);
        } else if (torsoCrits == 2) {
            jumpMP /= 2;
        }
        if (protoMek.hasWorkingMisc(MiscType.F_PARTIAL_WING)) {
            jumpMP += 2;
        }
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

    @Override
    protected int offensiveSpeedFactorMP() {
        // TM p306: The myomer booster is treated differently for defensive and offensive speeds
        // This would be the code for that:
//        int mp = (int) Math.ceil(walkMP() * 1.5);
//        if (protoMek.hasMyomerBooster()) {
//            mp++;
//        }
        // In accordance with Mordel's calculation, I'm not implementing this.
        return runMP + (int) Math.round(Math.max(jumpMP, umuMP) / 2.0);
    }
}
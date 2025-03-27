/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 2 or (at your option) any later version,
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
 */
package megamek.client.bot.caspar.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;
import megamek.common.Entity;

/**
 * Calculates the potential of the unit to act as a decoy
 * @author Luana Coppio
 */
public class DecoyPotentialCalculator extends BaseAxisCalculator {

    private final static int RUN_MP = 8;
    private final static int JUMP_MP = 6;
    private final static int DIVISOR_SLOW_UNITS = 8;
    private final static int DIVISOR_FAST_UNITS = 4;

    @Override
    public float[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates the potential of the unit to act as a decoy
        float[] decoyPotential = axis();
        var unit = pathing.getEntity();
        decoyPotential[0] = normalize(unit.getDamageLevel(), 0, getDivisor(unit));
        return decoyPotential;
    }

    /**
     * Returns the divisor to use for the normalization
     * @param unit the current unit
     * @return the divisor to use for the normalization
     */
    private static int getDivisor(Entity unit) {
        int divisor = DIVISOR_SLOW_UNITS;
        if ((unit.getJumpMP() >= JUMP_MP) || (unit.getRunMP() >= RUN_MP)) {
            divisor = DIVISOR_FAST_UNITS;
        }
        return divisor;
    }
}

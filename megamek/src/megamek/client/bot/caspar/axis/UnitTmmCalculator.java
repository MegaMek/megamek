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

/**
 * Calculates the unit TMM
 * @author Luana Coppio
 */
public class UnitTmmCalculator extends BaseAxisCalculator {
    @Override
    public float[] calculateAxis(Pathing pathing, GameState gameState) {
        // This is just an approximation
        float[] unitTmm = axis();
        int hexesMoved = pathing.getHexesMoved();
        boolean jumped = pathing.isJumping();
        boolean prone = pathing.getFinalProne();

        float tmm = jumped ? 1 : 0;
        tmm+= prone ? -2 : 0;

        if (hexesMoved >= 25) {
            tmm += 6;
        } else if (hexesMoved >= 18) {
            tmm += 5;
        } else if (hexesMoved >= 10) {
            tmm += 4;
        } else if (hexesMoved >= 7) {
            tmm += 3;
        } else if (hexesMoved >= 5) {
            tmm += 2;
        } else if (hexesMoved >= 3) {
            tmm += 1;
        } else if (hexesMoved == 0) {
            tmm -= 4;
        }
        unitTmm[0] = tmm;

        return unitTmm;
    }
}

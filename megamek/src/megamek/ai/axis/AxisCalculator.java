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
package megamek.ai.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;

/**
 * Interface for calculating specific input axes.
 * @author Luana Coppio
 */
public interface AxisCalculator {
    /**
     * Returns the number of values this axis calculator will produce.
     *
     * @return The number of values
     */
    default float[] axis() {
        return new float[1];
    }

    /**
     * Calculates one or more input axes for a movement path.
     *
     * @param pathing The movement path to evaluate
     * @return An array of normalized values (0-1)
     */
    float[] calculateAxis(Pathing pathing, GameState gameState);
}

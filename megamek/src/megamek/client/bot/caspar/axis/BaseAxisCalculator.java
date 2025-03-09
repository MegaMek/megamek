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


import megamek.ai.axis.AxisCalculator;

/**
 * Base class for axis calculators.
 * @author Luana Coppio
 */
public abstract class BaseAxisCalculator implements AxisCalculator {

    /**
     * Normalizes a value to the range [0d, 1d].
     *
     * @param value The value to normalize
     * @param min   The minimum expected value
     * @param max   The maximum expected value
     * @return A normalized value between 0 and 1
     */
    protected double normalize(double value, double min, double max) {
        if (max == min) {
            return 0.5d; // Avoid division by zero
        }
        double normalized = (value - min) / (max - min);
        return Math.max(0d, Math.min(1d, normalized));
    }
}

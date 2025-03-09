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
package megamek.client.bot.caspar;


import megamek.client.bot.caspar.axis.AxisType;
import megamek.client.bot.common.GameState;
import megamek.client.bot.caspar.axis.AxisCalculator;
import megamek.client.bot.caspar.axis.InputAxisCalculator;
import megamek.client.bot.common.Pathing;
import megamek.common.MovePath;

/**
 * Default implementation of the InputAxisCalculator interface.
 * Calculates all input axes as described in the CASPAR documentation.
 * @author Luana Coppio
 */
public class DefaultInputAxisCalculator implements InputAxisCalculator {
    // Registry of axis calculators
    private final AxisCalculator[] axisCalculators = AxisType.axisCalculators();
    private final double[] inputVector = new double[AxisType.totalAxisLength()];

    /**
     * Creates an input axis calculator with all required axis calculators.
     */
    public DefaultInputAxisCalculator() {
    }


    @Override
    public double[] calculateInputVector(Pathing movePath, GameState gameState) {
        // Start index for writing values
        int index = 0;

        // Calculate and insert each axis group
        for (AxisType axisType : AxisType.values()) {
            AxisCalculator calculator = axisCalculators[axisType.ordinal()];
            double[] axisValues = calculator.calculateAxis(movePath, gameState);

            // Copy values to the input vector
            System.arraycopy(axisValues, 0, inputVector, index, axisValues.length);
            index += axisValues.length;
        }

        return inputVector;
    }

    @Override
    public int getInputSize() {
        return inputVector.length;
    }
    // Example implementation for a few calculators
}




/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */
package megamek.utilities.ai;

import java.util.Arrays;

/**
 * This class represents a checkpoint in the optimization process.
 * It can be used to initialize the optimizer with a set of precalculated parameters.
 * @author Luana Coppio
 */
public class Checkpoint {
    private final double[] parameters;

    public Checkpoint(double[] parameters) {
        this.parameters = Arrays.copyOf(parameters, parameters.length);
    }

    public Checkpoint(ParameterOptimizer parameterOptimizer) {
        parameters = Arrays.copyOf(parameterOptimizer.finalParams, parameterOptimizer.finalParams.length);
    }

    public double[] getValues() {
        return Arrays.copyOf(parameters, parameters.length);
    }
}

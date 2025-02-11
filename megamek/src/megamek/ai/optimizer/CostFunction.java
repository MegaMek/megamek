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
package megamek.ai.optimizer;

import megamek.ai.dataset.UnitAction;
import megamek.ai.dataset.UnitState;

import java.util.Map;

/**
 * Represents a cost function for a unit action.
 * @author Luana Coppio
 */
public interface CostFunction {

    /**
     * Returns the number of parameters of the behavior function.
     * @return The number of parameters of the behavior function.
     */
    int numberOfParameters();

    /**
     * Resolves the cost of a unit action.
     * @param unitAction  The unit action to resolve.
     * @param currentUnitStates  The current state of the units.
     * @param parameters  The parameters of the behavior function.
     * @return The cost of the unit action.
     */
    double resolve(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, Parameters parameters);

    /**
     * Resolves the cost of a unit action.
     * @param unitAction  The unit action to resolve.
     * @param currentUnitStates  The current state of the units.
     * @param nextUnitState  The state of the units in the following round.
     * @param parameters The parameters of the behavior function.
     * @return The cost of the unit action.
     */
    default double resolve(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, Map<Integer, UnitState> nextUnitState, Parameters parameters) {
        return resolve(unitAction, currentUnitStates, parameters);
    }
}

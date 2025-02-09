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
package megamek.ai.dataset;


import java.util.List;

/**
 * Represents an action and the state of the board after the action is performed.
 * @param round
 * @param unitAction
 * @param boardUnitState
 * @author Luana Coppio
 */
public record ActionAndState(int round, UnitAction unitAction, List<UnitState> boardUnitState){}

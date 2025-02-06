/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common.autoresolve.component;

import megamek.common.AbstractPlayerTurn;
import megamek.common.autoresolve.acar.SimulationContext;

public abstract class AcTurn extends AbstractPlayerTurn {

    public AcTurn(int playerId) {
        super(playerId);
    }

    /**
     * Returns true when this turn can be played given the current state of the given game. Should
     * return false when e.g. no valid formation can be found to move for the player or the player or
     * formation of the turn is null (e.g. because it has previously been destroyed).
     *
     * @param game The game object
     * @return True when the turn can be played, false when it should be skipped
     */
    public abstract boolean isValid(SimulationContext game);
}

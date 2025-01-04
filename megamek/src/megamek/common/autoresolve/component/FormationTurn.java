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

import megamek.common.IGame;
import megamek.common.InGameObject;
import megamek.common.autoresolve.acar.SimulationContext;

public class FormationTurn extends AcTurn {

    /**
     * Creates a new player turn for an SBF Game.
     *
     * @param playerId The player who has to take action
     */
    public FormationTurn(int playerId) {
        super(playerId);
    }

    @Override
    public boolean isValid(SimulationContext game) {
        return (game.getPlayer(playerId()) != null) && game.hasEligibleFormation(this);
    }

    @Override
    public boolean isValidEntity(InGameObject unit, IGame game) {
        return (unit.getOwnerId() == playerId()) && unit instanceof Formation
            && ((Formation) unit).isEligibleForPhase(game.getPhase());
    }
}

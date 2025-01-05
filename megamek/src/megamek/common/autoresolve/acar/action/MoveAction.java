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
package megamek.common.autoresolve.acar.action;

import megamek.common.Coords;
import megamek.common.autoresolve.acar.SimulationContext;
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.acar.handler.MoveActionHandler;

public class MoveAction implements Action {

    private final int formationId;
    private final Coords destination;

    public MoveAction(int formationId, Coords destination) {
        this.formationId = formationId;
        this.destination = destination;
    }

    @Override
    public int getEntityId() {
        return formationId;
    }

    @Override
    public MoveActionHandler getHandler(SimulationManager gameManager) {
        return new MoveActionHandler(this, gameManager);
    }

    @Override
    public boolean isDataValid(SimulationContext context) {
        return context.getFormation(formationId).isPresent();
    }

    public Coords getDestination() {
        return destination;
    }

    @Override
    public String toString() {
        return "[MoveAction]: ID: " + formationId;
    }
}

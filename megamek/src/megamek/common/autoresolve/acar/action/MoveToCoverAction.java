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
import megamek.common.autoresolve.acar.handler.MoveToCoverActionHandler;

public class MoveToCoverAction implements Action {

    private final int formationId;
    private final int targetFormationId;
    private final Coords destination;

    public MoveToCoverAction(int formationId, int targetFormationId, Coords destination) {
        this.formationId = formationId;
        this.targetFormationId = targetFormationId;
        this.destination = destination;
    }

    @Override
    public int getEntityId() {
        return formationId;
    }

    public int getTargetFormationId() {
        return targetFormationId;
    }

    @Override
    public MoveToCoverActionHandler getHandler(SimulationManager gameManager) {
        return new MoveToCoverActionHandler(this, gameManager);
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

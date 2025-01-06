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
package megamek.common.autoresolve.acar.handler;

import megamek.common.BoardLocation;
import megamek.common.Coords;
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.acar.action.MoveAction;

public class MoveActionHandler extends AbstractActionHandler {

//    private final MoveReporter reporter;

    public MoveActionHandler(MoveAction action, SimulationManager gameManager) {
        super(action, gameManager);
//        this.reporter = new MoveReporter(gameManager.getGame(), this::addReport);
    }

    @Override
    public boolean cares() {
        return game().getPhase().isMovement();
    }

    @Override
    public void execute() {
        MoveAction moveAction = (MoveAction) getAction();
        var formationOpt = game().getFormation(moveAction.getEntityId());
        var movingFormation = formationOpt.orElseThrow();

        if (game().getBoardSize() < moveAction.getDestination().getX()) {
            simulationManager().setFormationAt(movingFormation, new BoardLocation(new Coords(game().getBoardSize()-1, 0), 0));
        } else if (moveAction.getDestination().getX() < 0) {
            simulationManager().setFormationAt(movingFormation, new BoardLocation(new Coords(0,0), 0));
        } else {
            simulationManager().setFormationAt(movingFormation, new BoardLocation(moveAction.getDestination(), 0));
        }
    }
}

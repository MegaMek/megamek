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
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.acar.action.MoveAction;
import megamek.common.autoresolve.acar.report.IMovementReport;
import megamek.common.autoresolve.acar.report.MovementReport;

public class MoveActionHandler extends AbstractActionHandler {

    private final IMovementReport reporter;

    public MoveActionHandler(MoveAction action, SimulationManager gameManager) {
        super(action, gameManager);
        this.reporter = MovementReport.create(gameManager);
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
        var x = movingFormation.getPosition().coords().getX();
        var moveX = moveAction.getDestination().getX();

        var targetFormationOpt = game().getFormation(moveAction.getTargetFormationId());
        var boardLocation = game().clamp(BoardLocation.of(moveAction.getDestination(), 0));

        if (targetFormationOpt.isPresent()) {
            var targetX = targetFormationOpt.get().getPosition().coords().getX();
            var moveDir = x < moveX ? 1 : -1;
            var targetDir = x < targetX ? 1 : -1;
            var relativeDir = moveDir * targetDir;

            reporter.reportMovement(movingFormation, targetFormationOpt.get(), relativeDir);
        } else {
            if (movingFormation.isWithdrawing()) {
                reporter.reportRetreatMovement(movingFormation);
            } else {
                reporter.reportMovement(movingFormation);
            }
        }
        movingFormation.getMemory().put("cover", false);
        simulationManager().setFormationAt(movingFormation, boardLocation);
    }
}

/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
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
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.autoResolve.acar.handler;

import megamek.common.autoResolve.acar.SimulationManager;
import megamek.common.autoResolve.acar.action.MoveToCoverAction;
import megamek.common.autoResolve.acar.report.IMovementReport;
import megamek.common.autoResolve.acar.report.MovementReport;
import megamek.common.board.BoardLocation;
import megamek.common.compute.Compute;

public class MoveToCoverActionHandler extends AbstractActionHandler {

    private final IMovementReport reporter;

    public MoveToCoverActionHandler(MoveToCoverAction action, SimulationManager gameManager) {
        super(action, gameManager);
        this.reporter = MovementReport.create(gameManager);
    }

    @Override
    public boolean cares() {
        return game().getPhase().isMovement();
    }

    @Override
    public void execute() {
        MoveToCoverAction moveAction = (MoveToCoverAction) getAction();

        var formationOpt = game().getFormation(moveAction.getEntityId());
        var movingFormation = formationOpt.orElseThrow();
        var x = movingFormation.getPosition().coords().getX();
        var moveX = moveAction.destination().getX();

        var targetFormationOpt = game().getFormation(moveAction.targetFormationId());
        var boardLocation = game().clamp(BoardLocation.of(moveAction.destination(), 0));

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

        // 70% chance of finding cover while walking
        movingFormation.getMemory().put("cover", Compute.randomFloat() < 0.7);
        simulationManager().setFormationAt(movingFormation, boardLocation);
    }
}

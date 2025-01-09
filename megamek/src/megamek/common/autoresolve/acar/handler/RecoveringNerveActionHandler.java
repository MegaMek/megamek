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

import megamek.common.Compute;
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.acar.action.RecoveringNerveAction;
import megamek.common.autoresolve.acar.action.RecoveringNerveActionToHitData;
import megamek.common.autoresolve.acar.report.IRecoveringNerveActionReporter;
import megamek.common.autoresolve.acar.report.RecoveringNerveActionReporter;
import megamek.common.autoresolve.component.Formation;

public class RecoveringNerveActionHandler extends AbstractActionHandler {

    private final IRecoveringNerveActionReporter report;

    public RecoveringNerveActionHandler(RecoveringNerveAction action, SimulationManager gameManager) {
        super(action, gameManager);
        this.report = RecoveringNerveActionReporter.create(gameManager);
    }

    @Override
    public boolean cares() {
        return game().getPhase().isEnd();
    }


    @Override
    public void execute() {
        var recoveringNerveAction = (RecoveringNerveAction) getAction();

        var formationOpt = game().getFormation(recoveringNerveAction.getEntityId());

        var formation = formationOpt.orElseThrow();
        if (formation.moraleStatus().ordinal() == 0) {
            return;
        }

        var toHit = RecoveringNerveActionToHitData.compileToHit(game(), recoveringNerveAction);
        report.reportRecoveringNerveStart(formation, toHit.getValue());
        var roll = Compute.rollD6(2);
        if (!roll.isTargetRollSuccess(toHit)) {
            var newMoraleStatus = Formation.MoraleStatus.values()[formation.moraleStatus().ordinal() -1];
            formation.setMoraleStatus(newMoraleStatus);
            report.reportMoraleStatusChange(newMoraleStatus, roll);
        } else {
            report.reportFailureRoll(roll, formation.moraleStatus());
        }
    }
}

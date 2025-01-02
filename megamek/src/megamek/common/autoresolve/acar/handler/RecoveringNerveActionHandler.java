/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 *  This file is part of MekHQ.
 *
 *  MekHQ is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MekHQ is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package megamek.common.autoresolve.acar.handler;

import megamek.common.Compute;
import mekhq.campaign.autoresolve.acar.SimulationManager;
import mekhq.campaign.autoresolve.acar.action.RecoveringNerveAction;
import mekhq.campaign.autoresolve.acar.action.RecoveringNerveActionToHitData;
import mekhq.campaign.autoresolve.acar.handler.AbstractActionHandler;
import mekhq.campaign.autoresolve.acar.report.RecoveringNerveActionReporter;
import mekhq.campaign.autoresolve.component.Formation;

public class RecoveringNerveActionHandler extends AbstractActionHandler {

    private final RecoveringNerveActionReporter report;

    public RecoveringNerveActionHandler(RecoveringNerveAction action, SimulationManager gameManager) {
        super(action, gameManager);
        this.report = new RecoveringNerveActionReporter(game(), this::addReport);
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

        report.reportRecoveringNerveStart(formation);
        var toHit = RecoveringNerveActionToHitData.compileToHit(game(), recoveringNerveAction);
        report.reportToHitValue(toHit.getValue());
        var roll = Compute.rollD6(2);
        if (!roll.isTargetRollSuccess(toHit)) {
            report.reportSuccessRoll(roll);
            var newMoraleStatus = Formation.MoraleStatus.values()[formation.moraleStatus().ordinal() -1];
            formation.setMoraleStatus(newMoraleStatus);
            report.reportMoraleStatusChange(newMoraleStatus);
        } else {
            report.reportFailureRoll(roll);
        }


    }
}

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
import megamek.common.Roll;
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.acar.action.MoraleCheckAction;
import megamek.common.autoresolve.acar.action.RecoveringNerveAction;
import megamek.common.autoresolve.acar.action.RecoveringNerveActionToHitData;
import megamek.common.autoresolve.acar.report.MoraleReporter;
import megamek.common.autoresolve.component.Formation;

public class MoraleCheckActionHandler extends AbstractActionHandler {

    private final MoraleReporter reporter;

    public MoraleCheckActionHandler(MoraleCheckAction action, SimulationManager gameManager) {
        super(action, gameManager);
        this.reporter = new MoraleReporter(gameManager.getGame(), this::addReport);
    }

    @Override
    public boolean cares() {
        return game().getPhase().isEnd();
    }

    @Override
    public void execute() {
        MoraleCheckAction moraleCheckAction = (MoraleCheckAction) getAction();

        var demoralizedOpt = game().getFormation(moraleCheckAction.getEntityId());
        var demoralizedFormation = demoralizedOpt.orElseThrow();
        var toHit = RecoveringNerveActionToHitData.compileToHit(game(), new RecoveringNerveAction(demoralizedFormation.getId()));
        Roll moraleCheck = Compute.rollD6(2);

        reporter.reportMoraleCheckStart(demoralizedFormation, toHit.getValue());
        reporter.reportMoraleCheckRoll(demoralizedFormation, moraleCheck);

        if (moraleCheck.isTargetRollSuccess(toHit)) {
            // Success - no morale worsening
            reporter.reportMoraleCheckSuccess(demoralizedFormation);
        } else {
            // Failure - morale worsens
            var oldMorale = demoralizedFormation.moraleStatus();
            if (Formation.MoraleStatus.values().length == oldMorale.ordinal() + 1) {
                demoralizedFormation.setMoraleStatus(Formation.MoraleStatus.ROUTED);
                reporter.reportMoraleCheckFailure(demoralizedFormation, oldMorale, Formation.MoraleStatus.ROUTED);
            } else {
                var newMorale = Formation.MoraleStatus.values()[oldMorale.ordinal() + 1];
                demoralizedFormation.setMoraleStatus(newMorale);
                reporter.reportMoraleCheckFailure(demoralizedFormation, oldMorale, newMorale);
            }
        }
    }
}

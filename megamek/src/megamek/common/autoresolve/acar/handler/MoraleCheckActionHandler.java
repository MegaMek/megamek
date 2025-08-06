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

package megamek.common.autoresolve.acar.handler;

import megamek.common.Compute;
import megamek.common.Roll;
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.acar.action.MoraleCheckAction;
import megamek.common.autoresolve.acar.action.RecoveringNerveAction;
import megamek.common.autoresolve.acar.action.RecoveringNerveActionToHitData;
import megamek.common.autoresolve.acar.report.IMoraleReporter;
import megamek.common.autoresolve.acar.report.MoraleReporter;
import megamek.common.autoresolve.component.Formation;

public class MoraleCheckActionHandler extends AbstractActionHandler {

    private final IMoraleReporter reporter;

    public MoraleCheckActionHandler(MoraleCheckAction action, SimulationManager gameManager) {
        super(action, gameManager);
        this.reporter = MoraleReporter.create(gameManager);
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
        var toHit = RecoveringNerveActionToHitData.compileToHit(game(),
              new RecoveringNerveAction(demoralizedFormation.getId()));
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

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
import megamek.common.autoresolve.acar.action.EngagementControlAction;
import megamek.common.autoresolve.acar.action.EngagementControlToHitData;
import megamek.common.autoresolve.acar.report.EngagementAndControlReporter;
import megamek.common.autoresolve.component.EngagementControl;

import java.util.Map;

public class EngagementAndControlActionHandler extends AbstractActionHandler {

    private final EngagementAndControlReporter reporter;

    public EngagementAndControlActionHandler(EngagementControlAction action, SimulationManager gameManager) {
        super(action, gameManager);
        this.reporter = new EngagementAndControlReporter(gameManager.getGame(), this::addReport);
    }

    @Override
    public boolean cares() {
        return game().getPhase().isMovement();
    }

    /**
     * Perform the engagement control action
     * This changes a little bit the "status quo" during the round, making units do or take less damage or evade specifically one unit
     */
    @Override
    public void execute() {
        EngagementControlAction engagementControl = (EngagementControlAction) getAction();

        var attackerOpt = game().getFormation(engagementControl.getEntityId());
        var targetOpt = game().getFormation(engagementControl.getTargetFormationId());
        var attacker = attackerOpt.orElseThrow();

        if (engagementControl.getEngagementControl().equals(EngagementControl.NONE)) {
            attacker.setEngagementControl(EngagementControl.NONE);
            return;
        }

        var target = targetOpt.orElseThrow();
        // Compute To-Hit
        var toHit = EngagementControlToHitData.compileToHit(game(), engagementControl);
        // Compute defender To-Hit as if roles reversed but same control
        var reverseAction = new EngagementControlAction(target.getId(), attacker.getId(), engagementControl.getEngagementControl());
        var toHitDefender = EngagementControlToHitData.compileToHit(game(), reverseAction);


        // Report the engagement start
        reporter.reportEngagementStart(attacker, target, engagementControl.getEngagementControl());

        // Report attacker to-hit
        reporter.reportAttackerToHitValue(toHit.getValue());

        Roll attackerRoll = Compute.rollD6(2);
        Roll defenderRoll = Compute.rollD6(2);

        // Report rolls
        reporter.reportAttackerRoll(attacker, attackerRoll);
        reporter.reportDefenderRoll(target, defenderRoll);

        var engagements = attacker.getMemory().getMemories("engagementControl");
        var targetEngagements = target.getMemory().getMemories("engagementControl");

        var attackerDelta = attackerRoll.getMarginOfSuccess(toHit);
        var defenderDelta = defenderRoll.getMarginOfSuccess(toHitDefender);

        attacker.setEngagementControl(engagementControl.getEngagementControl());
        attacker.setEngagementControlFailed(true);

        if (attackerDelta > defenderDelta) {
            attacker.setEngagementControlFailed(false);
            reporter.reportAttackerWin(attacker);

            switch (engagementControl.getEngagementControl()) {
                case NONE:
                    attacker.setEngagementControl(EngagementControl.NONE);
                    break;
                case FORCED_ENGAGEMENT:
                case EVADE:
                case OVERRUN:
                case STANDARD:
                    attacker.setTargetFormationId(target.getId());
                    target.setEngagementControl(engagementControl.getEngagementControl());
                    // Adding memory, so the unit can remember that it is engaged with the target
                    engagements.add(Map.of(
                        "targetFormationId", attacker.getId(),
                        "wonEngagementControl", false,
                        "attacker", true,
                        "engagementControl", engagementControl.getEngagementControl()
                    ));
                    // Adding memory, so the unit can remember that it is engaged with the attacker
                    targetEngagements.add(Map.of(
                        "targetFormationId", attacker.getId(),
                        "wonEngagementControl", false,
                        "attacker", false,
                        "engagementControl", engagementControl.getEngagementControl()
                    ));
            }
        } else {
            // Attacker loses
            reporter.reportAttackerLose(attacker);
            // Adding memory, so the unit can remember that it is engaged with the target
            engagements.add(Map.of(
                "targetFormationId", attacker.getId(),
                "wonEngagementControl", false,
                "attacker", true,
                "engagementControl", engagementControl.getEngagementControl()
            ));
            // Adding memory, so the unit can remember that it is engaged with the attacker
            targetEngagements.add(Map.of(
                "targetFormationId", attacker.getId(),
                "wonEngagementControl", false,
                "attacker", false,
                "engagementControl", engagementControl.getEngagementControl()
            ));
        }
    }
}

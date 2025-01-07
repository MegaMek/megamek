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
package megamek.common.autoresolve.acar.phase;

import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.acar.report.IInitiativePhaseHelperReporter;
import megamek.common.autoresolve.acar.report.InitiativePhaseHelperReporter;
import megamek.common.enums.GamePhase;

public class InitiativePhase  extends PhaseHandler {
    private final IInitiativePhaseHelperReporter reporter;

    public InitiativePhase(SimulationManager gameManager) {
        super(gameManager, GamePhase.INITIATIVE);
        reporter = InitiativePhaseHelperReporter.create(gameManager);
    }

    private void writeInitiativeReport() {
        reporter.writeInitiativeHeader();
        reporter.writeInitiativeRolls();
        reporter.writeFutureDeployment();
    }

    @Override
    protected void executePhase() {
        getSimulationManager().calculatePlayerInitialCounts();
        getSimulationManager().resetPlayersDone();
        getSimulationManager().rollInitiative();
        getSimulationManager().incrementAndSendGameRound();
        writeInitiativeReport();
    }
}

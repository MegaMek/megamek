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
package megamek.common.autoresolve.acar.manager;

import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.acar.report.PublicReportEntry;
import megamek.common.enums.GamePhase;

public record PhaseEndManager(SimulationManager simulationManager) implements SimulationManagerHelper {

    public void managePhase() {
        switch (simulationManager.getGame().getPhase()) {
            case INITIATIVE:
                simulationManager.addReport(new PublicReportEntry(999));
                simulationManager.getGame().setupDeployment();
                simulationManager.resetFormations();
                simulationManager.flushPendingReports();
                if (simulationManager.getGame().shouldDeployThisRound()) {
                    simulationManager.changePhase(GamePhase.DEPLOYMENT);
                } else {
                    simulationManager.changePhase(GamePhase.SBF_DETECTION);
                }
                break;
            case DEPLOYMENT:
                simulationManager.addReport(new PublicReportEntry(999));
                simulationManager.getGame().clearDeploymentThisRound();
                phaseCleanup();
                simulationManager.changePhase(GamePhase.SBF_DETECTION);
                break;
            case SBF_DETECTION:
                simulationManager.getActionsProcessor().handleActions();
                phaseCleanup();
                simulationManager.changePhase(GamePhase.MOVEMENT);
                break;
            case MOVEMENT:
                simulationManager.addReport(new PublicReportEntry(999));
                simulationManager.addReport(new PublicReportEntry(2201));
                simulationManager.getActionsProcessor().handleActions();
                phaseCleanup();
                simulationManager.changePhase(GamePhase.FIRING);
                break;
            case FIRING:
                simulationManager.addReport(new PublicReportEntry(999));
                simulationManager.addReport(new PublicReportEntry(2002));
                simulationManager.getActionsProcessor().handleActions();
                phaseCleanup();
                simulationManager.changePhase(GamePhase.END);
                break;
            case END:
                simulationManager.getActionsProcessor().handleActions();
                endPhaseCleanup();
                if (simulationManager.isVictory()) {
                    simulationManager.changePhase(GamePhase.VICTORY);
                }
                break;
            case VICTORY:
                endPhaseCleanup();
            case STARTING_SCENARIO:
            default:
                break;
        }
    }

    private void phaseCleanup() {
        simulationManager.resetPlayersDone();
        simulationManager.resetFormationsDone();
        simulationManager.flushPendingReports();
    }
    private void endPhaseCleanup() {
        simulationManager.resetPlayersDone();
        simulationManager.resetFormations();
        simulationManager.flushPendingReports();
    }

}

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

import megamek.common.autoresolve.acar.SimulationContext;
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.acar.report.IPhaseEndReporter;
import megamek.common.autoresolve.acar.report.PhaseEndReporter;
import megamek.common.enums.GamePhase;

public class PhaseEndManager implements SimulationManagerHelper {
    private final SimulationManager simulationManager;
    private final IPhaseEndReporter reporter;

    public PhaseEndManager(SimulationManager simulationManager) {
        this.simulationManager = simulationManager;
        this.reporter = PhaseEndReporter.create(simulationManager);
    }

    public void managePhase() {
        switch (simulationManager.getGame().getPhase()) {
            case STARTING_SCENARIO:
                break;
            case INITIATIVE:
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
                simulationManager.getGame().clearDeploymentThisRound();
                phaseCleanup();
                simulationManager.changePhase(GamePhase.SBF_DETECTION);
                break;
            case SBF_DETECTION: // this is being used just as a "pre movement phase", we dont actually handle sbf detection
                simulationManager.getActionsProcessor().handleActions();
                phaseCleanup();
                // Print the movement phase header before entering in it
                reporter.movementPhaseHeader();

                simulationManager.changePhase(GamePhase.MOVEMENT);
                break;
            case MOVEMENT:
                simulationManager.getActionsProcessor().handleActions();
                phaseCleanup();
                simulationManager.changePhase(GamePhase.FIRING);
                break;
            case FIRING:
                reporter.firingPhaseHeader();
                simulationManager.getActionsProcessor().handleActions();
                phaseCleanup();
                simulationManager.changePhase(GamePhase.END);
                break;
            case END:
                simulationManager.getActionsProcessor().handleActions();
                simulationManager.resetInitiative();
                endPhaseCleanup();
                if (simulationManager.isVictory()) {
                    simulationManager.changePhase(GamePhase.VICTORY);
                }
                break;
            case VICTORY:
                endPhaseCleanup();
                reporter.closeTheFile();
                reporter.addSummary(simulationManager());
                break;
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

    @Override
    public SimulationManager simulationManager() {
        return simulationManager;
    }

    @Override
    public SimulationContext game() {
        return simulationManager.getGame();
    }
}

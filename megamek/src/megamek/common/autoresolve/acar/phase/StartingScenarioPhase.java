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

package megamek.common.autoresolve.acar.phase;

import megamek.common.enums.GamePhase;
import mekhq.campaign.autoresolve.acar.SimulationManager;
import mekhq.campaign.autoresolve.acar.report.StartingScenarioPhaseReporter;

public class StartingScenarioPhase extends PhaseHandler {

    public StartingScenarioPhase(SimulationManager gameManager) {
        super(gameManager, GamePhase.STARTING_SCENARIO);
    }

    @Override
    protected void executePhase() {
        getSimulationManager().resetPlayersDone();
        getSimulationManager().resetFormations();
        getSimulationManager().calculatePlayerInitialCounts();
        getSimulationManager().getGame().setupTeams();
        getSimulationManager().getGame().setupDeployment();

        var reporter = new StartingScenarioPhaseReporter(getSimulationManager().getGame(), getSimulationManager()::addReport);
        reporter.startingScenarioHeader();
        reporter.formationsSetup(getSimulationManager());
    }
}

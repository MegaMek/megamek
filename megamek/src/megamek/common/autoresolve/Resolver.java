/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.autoresolve;

import megamek.common.Entity;
import megamek.common.Player;
import megamek.common.autoresolve.acar.SimulationOptions;
import megamek.common.autoresolve.converter.SetupForces;
import megamek.common.options.AbstractOptions;
import mekhq.campaign.Campaign;
import mekhq.campaign.autoresolve.acar.SimulationContext;
import mekhq.campaign.autoresolve.acar.SimulationManager;
import mekhq.campaign.autoresolve.acar.SimulationOptions;
import mekhq.campaign.autoresolve.converter.SetupForces;
import mekhq.campaign.autoresolve.event.AutoResolveConcludedEvent;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.unit.Unit;

import java.util.List;


/**
 * @author Luana Coppio
 */
public class Resolver {

    private final SimulationOptions options;
    private final SetupForces setupForces;

    public Resolver(List<Player> playes,
                    List<Entity> units,
                    AbstractOptions gameOptions) {

        this.options = new SimulationOptions(gameOptions);
        this.setupForces = new SetupForces(campaign, units, scenario);
    }

    public AutoResolveConcludedEvent resolveSimulation() {
        SimulationContext context = new SimulationContext(scenario, options, setupForces);
        SimulationManager simulationManager = new SimulationManager(context);
        initializeGameManager(simulationManager);
        simulationManager.execute();
        return simulationManager.getConclusionEvent();
    }

    private void initializeGameManager(SimulationManager simulationManager) {
        simulationManager.addPhaseHandler(new StartingScenarioPhase(simulationManager));
        simulationManager.addPhaseHandler(new InitiativePhase(simulationManager));
        simulationManager.addPhaseHandler(new DeploymentPhase(simulationManager));
        simulationManager.addPhaseHandler(new MovementPhase(simulationManager));
        simulationManager.addPhaseHandler(new FiringPhase(simulationManager));
        simulationManager.addPhaseHandler(new EndPhase(simulationManager));
        simulationManager.addPhaseHandler(new VictoryPhase(simulationManager));
    }
}

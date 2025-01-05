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
package megamek.common.autoresolve;

import megamek.common.Board;
import megamek.common.autoresolve.acar.SimulationContext;
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.acar.SimulationOptions;
import megamek.common.autoresolve.acar.phase.*;
import megamek.common.autoresolve.converter.SetupForces;
import megamek.common.autoresolve.event.AutoResolveConcludedEvent;
import megamek.common.options.AbstractOptions;


/**
 * @author Luana Coppio
 */
public class Resolver {

    private final SimulationOptions options;
    private final SetupForces setupForces;
    private final Board board;

    public Resolver(SetupForces setupForces,
                    AbstractOptions gameOptions, Board board) {
        this.options = new SimulationOptions(gameOptions);
        this.setupForces = setupForces;
        this.board = board;
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

    public AutoResolveConcludedEvent resolveSimulation() {
        SimulationContext context = new SimulationContext(options, setupForces, board);
        SimulationManager simulationManager = new SimulationManager(context);
        initializeGameManager(simulationManager);
        simulationManager.execute();
        return simulationManager.getConclusionEvent();
    }

}

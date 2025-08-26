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
package megamek.common.autoResolve;

import megamek.common.autoResolve.acar.SimulationContext;
import megamek.common.autoResolve.acar.SimulationManager;
import megamek.common.autoResolve.acar.SimulationOptions;
import megamek.common.autoResolve.acar.phase.DeploymentPhase;
import megamek.common.autoResolve.acar.phase.EndPhase;
import megamek.common.autoResolve.acar.phase.FiringPhase;
import megamek.common.autoResolve.acar.phase.InitiativePhase;
import megamek.common.autoResolve.acar.phase.MovementPhase;
import megamek.common.autoResolve.acar.phase.StartingScenarioPhase;
import megamek.common.autoResolve.acar.phase.VictoryPhase;
import megamek.common.autoResolve.converter.SetupForces;
import megamek.common.autoResolve.event.AutoResolveConcludedEvent;
import megamek.common.board.Board;
import megamek.common.options.AbstractOptions;
import megamek.common.planetaryConditions.PlanetaryConditions;


/**
 * This class is responsible for resolving the simulation of a battle
 *
 * @author Luana Coppio
 */
public class Resolver {

    private final SimulationOptions options;
    private final SetupForces setupForces;
    private final Board board;
    private final PlanetaryConditions planetaryConditions;
    private final boolean suppressLog;

    /**
     * Constructor for Resolver
     *
     * @param setupForces         the {@link SetupForces} object that converts MegaMek forces to ACAR forces
     * @param gameOptions         the {@link megamek.common.options.GameOptions}
     * @param board               the {@link Board}
     * @param planetaryConditions the {@link PlanetaryConditions}
     * @param suppressLog         whether to suppress log output, important to keep memory usage low during parallel
     *                            simulation
     */
    public Resolver(SetupForces setupForces, AbstractOptions gameOptions, Board board,
          PlanetaryConditions planetaryConditions, boolean suppressLog) {
        this.options = new SimulationOptions(gameOptions);
        this.setupForces = setupForces;
        this.board = board;
        this.planetaryConditions = planetaryConditions;
        this.suppressLog = suppressLog;
    }


    /**
     * Instantiates an Auto Resolver object
     *
     * @param setupForces         the {@link SetupForces} object that converts MegaMek forces to ACAR forces
     * @param gameOptions         the {@link megamek.common.options.GameOptions}
     * @param board               the {@link Board}
     * @param planetaryConditions the {@link PlanetaryConditions}
     *
     * @return a new {@link Resolver} object ready to run
     */
    public static Resolver simulationRun(SetupForces setupForces, AbstractOptions gameOptions, Board board,
          PlanetaryConditions planetaryConditions) {
        return new Resolver(setupForces, gameOptions, board, planetaryConditions, false);
    }

    /**
     * Instantiates an Auto Resolver object without log output
     *
     * @param setupForces         the {@link SetupForces} object that converts MegaMek forces to ACAR forces
     * @param gameOptions         the {@link megamek.common.options.GameOptions}
     * @param board               the {@link Board}
     * @param planetaryConditions the {@link PlanetaryConditions}
     *
     * @return a new {@link Resolver} object with log output suppressed ready to run
     */
    public static Resolver simulationRunWithoutLog(SetupForces setupForces, AbstractOptions gameOptions, Board board,
          PlanetaryConditions planetaryConditions) {
        return new Resolver(setupForces, gameOptions, board, planetaryConditions, true);
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

    /**
     * This method runs the simulation and returns the result event {@link AutoResolveConcludedEvent}
     *
     * @return an {@link AutoResolveConcludedEvent} with the detailed result of the simulation, which can be used for
     *       further processing
     */
    public AutoResolveConcludedEvent resolveSimulation() {
        SimulationContext context = new SimulationContext(options, setupForces, board, planetaryConditions);
        SimulationManager simulationManager = new SimulationManager(context, suppressLog);
        initializeGameManager(simulationManager);
        return simulationManager.execute();
    }

}

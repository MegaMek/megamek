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
    private final boolean suppressLog;

    public Resolver(SetupForces setupForces,
                    AbstractOptions gameOptions, Board board, boolean suppressLog) {
        this.options = new SimulationOptions(gameOptions);
        this.setupForces = setupForces;
        this.board = board;
        this.suppressLog = suppressLog;
    }

    public static Resolver simulationRun(SetupForces setupForces, AbstractOptions gameOptions, Board board) {
        return new Resolver(setupForces, gameOptions, board, false);
    }

    public static Resolver simulationRunWithoutLog(SetupForces setupForces, AbstractOptions gameOptions, Board board) {
        return new Resolver(setupForces, gameOptions, board, true);
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
        SimulationManager simulationManager = new SimulationManager(context, suppressLog);
        initializeGameManager(simulationManager);
        return simulationManager.execute();
    }

}

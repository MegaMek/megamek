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

import megamek.common.autoresolve.acar.SimulationContext;
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.enums.GamePhase;

/**
 * @author Luana Coppio
 */
public abstract class PhaseHandler {

    private final GamePhase phase;
    private final SimulationManager simulationManager;

    public PhaseHandler(SimulationManager simulationManager, GamePhase phase) {
        this.phase = phase;
        this.simulationManager = simulationManager;
    }

    private boolean isPhase(GamePhase phase) {
        return this.phase == phase;
    }

    protected SimulationManager getSimulationManager() {
        return simulationManager;
    }

    protected SimulationContext getContext() {
        return getSimulationManager().getGame();
    }

    public void execute() {
        if (isPhase(getSimulationManager().getGame().getPhase())) {
            executePhase();
        }
    }

    protected abstract void executePhase();
}

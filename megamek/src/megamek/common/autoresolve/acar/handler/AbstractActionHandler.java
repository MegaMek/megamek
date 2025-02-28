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

import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.acar.action.Action;
import megamek.common.autoresolve.acar.action.ActionHandler;

public abstract class AbstractActionHandler implements ActionHandler {

    private final Action action;
    private final SimulationManager gameManager;
    private boolean isFinished = false;

    public AbstractActionHandler(Action action, SimulationManager gameManager) {
        this.action = action;
        this.gameManager = gameManager;
    }

    @Override
    public boolean isFinished() {
        return isFinished;
    }

    @Override
    public void setFinished() {
        isFinished = true;
    }

    @Override
    public Action getAction() {
        return action;
    }

    @Override
    public SimulationManager simulationManager() {
        return gameManager;
    }
}

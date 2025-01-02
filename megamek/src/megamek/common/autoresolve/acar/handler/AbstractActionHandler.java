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

package megamek.common.autoresolve.acar.handler;

import mekhq.campaign.autoresolve.acar.SimulationManager;
import mekhq.campaign.autoresolve.acar.action.Action;
import mekhq.campaign.autoresolve.acar.action.ActionHandler;

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

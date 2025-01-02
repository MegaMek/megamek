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

package megamek.common.autoresolve.acar.manager;

import megamek.common.actions.EntityAction;
import mekhq.campaign.autoresolve.acar.SimulationManager;
import mekhq.campaign.autoresolve.acar.manager.SimulationManagerHelper;

public record ActionsProcessor(SimulationManager simulationManager) implements SimulationManagerHelper {

    public void handleActions() {
        addNewHandlers();
        processHandlers();
        removeFinishedHandlers();
    }

    /**
     * Add new handlers to the game
     * Every new type of action has to have its handler registered here, otherwise it won't be processed
     */
    private void addNewHandlers() {
        for (EntityAction action : game().getActionsVector()) {
            if (action instanceof AttackAction attack && attack.getHandler(simulationManager) != null) {
                game().addActionHandler(attack.getHandler(simulationManager));
            } else if (action instanceof EngagementControlAction engagementControl && engagementControl.getHandler(simulationManager) != null) {
                game().addActionHandler(engagementControl.getHandler(simulationManager));
            } else if (action instanceof WithdrawAction withdraw && withdraw.getHandler(simulationManager) != null) {
                game().addActionHandler(withdraw.getHandler(simulationManager));
            } else if (action instanceof RecoveringNerveAction recoveringNerve && recoveringNerve.getHandler(simulationManager) != null) {
                game().addActionHandler(recoveringNerve.getHandler(simulationManager));
            } else if (action instanceof MoraleCheckAction moraleCheck && moraleCheck.getHandler(simulationManager) != null) {
                game().addActionHandler(moraleCheck.getHandler(simulationManager));
            }
        }
    }

    private void processHandlers() {
        for (ActionHandler handler : game().getActionHandlers()) {
            if (handler.cares()) {
                handler.handle();
            }
        }
    }

    private void removeFinishedHandlers() {
        game().getActionHandlers().removeIf(ActionHandler::isFinished);
    }
}

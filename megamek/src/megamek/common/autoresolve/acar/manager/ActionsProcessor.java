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

import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.acar.action.Action;
import megamek.common.autoresolve.acar.action.ActionHandler;
import megamek.logging.MMLogger;

public record ActionsProcessor(SimulationManager simulationManager) implements SimulationManagerHelper {
    private static final MMLogger logger = MMLogger.create(ActionsProcessor.class);

    public void handleActions() {
        addNewHandlers();
        processHandlers();
        removeFinishedHandlers();
    }

    /**
     * Add new handlers to the game
     */
    private void addNewHandlers() {
        for (Action action : game().getActionsVector()) {
            var actionHandler = action.getHandler(simulationManager);
            if (actionHandler != null) {
                game().addActionHandler(actionHandler);
            } else {
                logger.error("Action " + action + " has no handler");
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

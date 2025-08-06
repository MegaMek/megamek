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

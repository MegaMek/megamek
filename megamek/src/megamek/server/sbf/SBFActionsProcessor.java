/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.server.sbf;

import megamek.common.actions.EntityAction;
import megamek.common.actions.sbf.SBFAttackAction;

public record SBFActionsProcessor(SBFGameManager gameManager) implements SBFGameManagerHelper {

    void handleActions() {
        addNewHandlers();
        processHandlers();
        removeFinishedHandlers();
    }

    private void addNewHandlers() {
        for (EntityAction action : game().getActionsVector()) {
            if (action instanceof SBFAttackAction attack && attack.getHandler(gameManager) != null) {
                game().addActionHandler(attack.getHandler(gameManager));
            }
            // Actions that arent attacks are currently not processed through handlers but maybe in the future?
        }
    }

    private void processHandlers() {
        for (SBFActionHandler handler : game().getActionHandlers()) {
            if (handler.cares()) {
                handler.handle();
            }
        }
    }

    private void removeFinishedHandlers() {
        game().getActionHandlers().removeIf(SBFActionHandler::isFinished);
    }
}

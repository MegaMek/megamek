/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

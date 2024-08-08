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
package megamek.common.actions.sbf;

import megamek.common.actions.EntityAction;
import megamek.common.strategicBattleSystems.SBFGame;
import megamek.server.sbf.SBFActionHandler;
import megamek.server.sbf.SBFGameManager;

/**
 * This interface is implemented by actions of units (mostly, formations) in SBF games.
 */
public interface SBFAction extends EntityAction {

    /**
     * @return A handler that will process this action as an extension of the SBFGameManager
     */
    SBFActionHandler getHandler(SBFGameManager gameManager);

    /**
     * Validates the data of this action. Validation should not check game rule details, only if the action
     * can be handled without running into missing or false data (NullPointerExceptions). Errors should
     * be logged. The action will typically be ignored and removed if validation fails.
     *
     * @param game The game
     * @return true when this action is valid, false otherwise
     */
    boolean isDataValid(SBFGame game);
}

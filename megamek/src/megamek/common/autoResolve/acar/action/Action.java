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

package megamek.common.autoResolve.acar.action;

import megamek.common.actions.EntityAction;
import megamek.common.autoResolve.acar.SimulationContext;
import megamek.common.autoResolve.acar.SimulationManager;

/**
 * @author Luana Coppio
 */
public interface Action extends EntityAction {

    /**
     * @return A handler that will process this action as an extension of the SBFGameManager
     */
    ActionHandler getHandler(SimulationManager gameManager);

    /**
     * Validates the data of this action. Validation should not check game rule details, only if the action can be
     * handled without running into missing or false data (NullPointerExceptions). Errors should be logged. The action
     * will typically be ignored and removed if validation fails.
     *
     * @param context The simulation context
     *
     * @return true when this action is valid, false otherwise
     */
    boolean isDataValid(SimulationContext context);

    default boolean isInvalid(SimulationContext context) {
        return !isDataValid(context);
    }

}

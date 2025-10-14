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

/**
 * This interface is implemented by SBF's action handlers, typically attack handlers. As in TW, action handlers are
 * extensions to the GameManager that handle all aspects of an action including modifying the game object and sending
 * packets upon being called by the GameManager.
 */
public interface SBFActionHandler extends SBFGameManagerHelper {

    /**
     * @return True when this handler should be called at the present state of the SBFGame (e.g. the phase). In that
     *       case, {@link #handle()} should be called.
     */
    boolean cares();

    /**
     * Handles the action, e.g. attack with everything that's necessary, such as adding a report and sending changes to
     * the Clients. When the handler has finished handling the action and is no longer needed, it must call
     * {@link #setFinished()} to mark itself as a candidate for removal.
     */
    void handle();

    void setFinished();

    /**
     * If it returns true, it must be removed from the list of active handlers (handling this action is finished
     * entirely). If it returns false, it must remain.
     *
     * @return False when this handler must remain active after doing its present handling, true otherwise
     */
    boolean isFinished();

    /**
     * @return The EntityAction that this handler is executing.
     */
    EntityAction getAction();
}

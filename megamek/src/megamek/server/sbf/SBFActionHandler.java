/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General License for more details.
 *
 * You should have received a copy of the GNU General License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.server.sbf;

import megamek.common.actions.EntityAction;

/**
 * This interface is implemented by SBF's action handlers, typically attack handlers. As in TW,
 * action handlers are extensions to the GameManager that handle all aspects of an action including
 * modifying the game object and sending packets upon being called by the GameManager.
 */
public interface SBFActionHandler extends SBFGameManagerHelper {

    /**
     * @return True when this handler should be called at the present state of the SBFGame (e.g. the phase).
     * In that case, {@link #handle()} should be called.
     */
    boolean cares();

    /**
     * Handles the action, e.g. attack with everything that's necessary, such as adding a report and sending
     * changes to the Clients. When the handler has finished handling the action and is no longer needed,
     * it must call {@link #setFinished()} to mark itself as a candidate for removal.
     */
    void handle();

    void setFinished();

    /**
     * If it returns true, it must be removed from the list of active handlers
     *  (handling this action is finished entirely). If it returns false, it must remain.
     *
     * @return False when this handler must remain active after doing its present handling, true otherwise
     */
    boolean isFinished();

    /**
     * @return The EntityAction that this handler is executing.
     */
    EntityAction getAction();
}
/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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

package megamek.ai.utility;

public abstract class AbstractAction {
    // Actions with target are scored per target

    private final String actionName;
    private final long actionId;

    public AbstractAction(String actionName, long actionId) {
        this.actionName = actionName;
        this.actionId = actionId;
    }

    public String getActionName() {
        return actionName;
    }

    public long getActionId() {
        return actionId;
    }
}

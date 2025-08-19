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

package megamek.common.event;

import java.io.Serial;

import megamek.common.actions.EntityAction;

/**
 * Instances of this class are sent when a strategic action is created in the game
 *
 * @see GameListener
 */
public class GamePlayerStrategicActionEvent extends GameEvent {

    @Serial
    private static final long serialVersionUID = 928848699583079097L;
    protected EntityAction action;

    /**
     * Construct new GameNewActionEvent
     *
     * @param source sender
     */
    public GamePlayerStrategicActionEvent(Object source, EntityAction action) {
        super(source);
        this.action = action;
    }

    /**
     * @return the action.
     */
    public EntityAction getAction() {
        return action;
    }

    @Override
    public void fireEvent(GameListener gl) {
        gl.gamePlayerStrategicAction(this);
    }

    @Override
    public String getEventName() {
        return "Game New Action";
    }
}

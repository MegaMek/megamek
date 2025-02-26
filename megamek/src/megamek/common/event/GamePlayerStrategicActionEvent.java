/*
 * MegaMek - Copyright (C) 2024 - The MegaMek Team
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
package megamek.common.event;

import megamek.common.actions.EntityAction;

import java.io.Serial;

/**
 * Instances of this class are sent when an strategic action is created in the game
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
     * @param action
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

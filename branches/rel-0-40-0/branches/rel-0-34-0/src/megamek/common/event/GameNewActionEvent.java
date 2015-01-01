/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

/**
 * Instances of this class are sent when new Action added to the game
 * 
 * @see GameListener
 */
public class GameNewActionEvent extends GameEvent {

    /**
     * 
     */
    private static final long serialVersionUID = 928848699583079097L;
    protected EntityAction action;

    /**
     * Cunstruct new GameNewActionEvent
     * 
     * @param source sender
     * @param action
     */
    public GameNewActionEvent(Object source, EntityAction action) {
        super(source, GAME_NEW_ACTION);
        this.action = action;
    }

    /**
     * @return the action.
     */
    public EntityAction getAction() {
        return action;
    }

}

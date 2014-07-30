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

import megamek.common.IGame;

/**
 * An event that is fired at the end of the victory phase, before the game state
 * is reset.  It can be used to retrieve information from the game before the
 * state is reset and the lounge phase begins.
 * 
 * @see IGame#end(int, int)
 * @see GameListener
 */
public class GameVictoryEvent extends GameEvent {

    /**
     * 
     */
    private static final long serialVersionUID = -8470655646019563063L;

    /**
     * @param source event source
     */
    public GameVictoryEvent(Object source) {
        super(source);
    }

    @Override
    public void fireEvent(GameListener gl) {
        gl.gameVictory(this);
    }

    @Override
    public String getEventName() {
        return "Game Victory";
    }
}

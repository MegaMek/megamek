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
 * Instances of this class are sent when the game finished
 * 
 * @see IGame#end(int, int)
 * @see GameListener
 */
public class GameEndEvent extends GameEvent {

    /**
     * 
     */
    private static final long serialVersionUID = -8470655646019563063L;

    /**
     * @param source event source
     */
    public GameEndEvent(Object source) {
        super(source);
    }

    @Override
    public void fireEvent(GameListener gl) {
        gl.gameEnd(this);
    }

    @Override
    public String getEventName() {
        return "Game End";
    }
}
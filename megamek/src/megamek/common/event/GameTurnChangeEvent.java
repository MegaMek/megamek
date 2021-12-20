/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.event;

import megamek.common.Player;

/**
 * Instances of this class are sent when Game turn changes.  This even keeps track of the player who
 * will be taking the new turn as well as the player who took the turn that triggered this event.
 */
public class GameTurnChangeEvent extends GamePlayerEvent {
    private static final long serialVersionUID = -6812056631576383917L;

    /**
     * Track the ID of the player who took the turn that triggered this even.
     */
    private int prevPlayerId;

    /**
     * @param source
     *            The Game instance
     * @param currPlayer
     *            The player for whom the new turn is for.
     * @param prevPlayerId
     *            The id of the player who took the turn that triggered this
     *            event.
     */
    public GameTurnChangeEvent(Object source, Player currPlayer, int prevPlayerId) {
        super(source, currPlayer);
        this.prevPlayerId = prevPlayerId;
    }

    @Override
    public void fireEvent(GameListener gl) {
        gl.gameTurnChange(this);    
    }

    @Override
    public String getEventName() {
        return "Turn Change";
    }
    
    public int getPreviousPlayerId() {
        return prevPlayerId;
    }
}

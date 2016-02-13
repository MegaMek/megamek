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

import megamek.common.IPlayer;

/**
 * Instances of this class are sent when chat message received
 */
public class GamePlayerChatEvent extends GamePlayerEvent {

    /**
     * 
     */
    private static final long serialVersionUID = 9077796386452985153L;
    protected String message;

    /**
     * @param source
     * @param player
     * @param message
     */
    public GamePlayerChatEvent(Object source, IPlayer player, String message) {
        super(source, player);
        this.message = message;
    }

    /**
     * @return the chat message.
     */
    public String getMessage() {
        return message;
    }

    @Override
    public void fireEvent(GameListener gl) {
        gl.gamePlayerChat(this);
    }
    
    @Override
    public String getEventName() {
        return "Chat";
    }
}

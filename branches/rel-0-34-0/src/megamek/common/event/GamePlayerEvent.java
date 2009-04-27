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

import megamek.common.Player;

/**
 * Instances of descendant classes are sent as a result of Game changes related
 * to Players
 * 
 * @see GamePlayerChangeEvent
 * @see GamePlayerChatEvent
 * @see GameListener
 */
public abstract class GamePlayerEvent extends GameEvent {

    /**
     * 
     */
    private static final long serialVersionUID = -3259778708415623296L;
    protected Player player;

    /**
     * @param source
     * @param player
     * @param type
     */
    public GamePlayerEvent(Object source, Player player, int type) {
        super(source, type);
        this.player = player;
    }

    /**
     * @return the player.
     */
    public Player getPlayer() {
        return player;
    }
}

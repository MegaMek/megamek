/*
 * MegaMek - Copyright (C) 2005,2006 Ben Mazur (bmazur@sev.org)
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
 * Instances of this class are sent when Game turn changes   
 */
public class GameTurnChangeEvent extends GamePlayerEvent {
    static final long serialVersionUID = -6812056631576383917L;
    
    /**
     * @param source
     * @param player
     */
    public GameTurnChangeEvent(Object source, Player player) {
        super(source, player, GAME_TURN_CHANGE);
    }
    
}

/**
 * MegaMek - Copyright (C) 2000,2001,2002,2004 Ben Mazur (bmazur@sev.org)
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

package megamek.client;

import megamek.common.*;

public class GameEvent extends java.util.EventObject {
    //public static final int        GAME_PLAYER_CONNECTED          = 0;
    public static final int GAME_PLAYER_DISCONNECTED = 1;
    public static final int GAME_PLAYER_STATUSCHANGE = 2;

    public static final int GAME_PLAYER_CHAT = 3;

    public static final int GAME_PHASE_CHANGE = 4;
    public static final int GAME_TURN_CHANGE = 5;

    public static final int GAME_NEW_ENTITIES = 6;
    public static final int GAME_NEW_SETTINGS = 7;
    public static final int GAME_BOARD_CHANGE = 8;
    public static final int GAME_END = 9;
    public static final int GAME_REPORT = 10;
    public static final int GAME_MAP_QUERY = 11;

    protected int type;
    protected Player player;
    protected String message;

    /**
     * Construct game event
     */
    public GameEvent(Object source, int type, Player player, String message) {
        super(source);
        this.type = type;
        this.player = player;
        this.message = message;
    }

    /**
     * Return the player associated with this event, or null if
     * not applicable.
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Return the message associated with this event, or null
     * if not applicable.
     */
    public String getMessage() {
        return message;
    }

    public int getType() {
        return type;
    }

    public String toString() {
        StringBuffer buff = new StringBuffer();
        switch (this.type) {
            case GAME_PLAYER_STATUSCHANGE :
                buff.append("Status Change"); //$NON-NLS-1$
                break;
            case GAME_PLAYER_CHAT :
                buff.append("Chat"); //$NON-NLS-1$
                break;
            case GAME_PHASE_CHANGE :
                buff.append("Phase Change"); //$NON-NLS-1$
                break;
            case GAME_TURN_CHANGE :
                buff.append("Turn Change"); //$NON-NLS-1$
                break;
            case GAME_NEW_ENTITIES :
                buff.append("New Entities"); //$NON-NLS-1$
                break;
            case GAME_NEW_SETTINGS :
                buff.append("New Settings"); //$NON-NLS-1$
                break;
            default :
                buff.append("Unknown"); //$NON-NLS-1$
                break;
        }
        buff.append(" game event "); //$NON-NLS-1$
        if (null != this.player) {
            buff.append("for ").append(this.player.getName()).append(" (id: ").append(this.player.getId()).append(") "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
        if (null != this.message) {
            buff.append(this.message);
        }
        return buff.toString();
    }

}

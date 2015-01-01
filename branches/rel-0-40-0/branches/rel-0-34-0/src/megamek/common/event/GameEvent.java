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

/**
 * Instances of descendant classes are sent as a result of Game change
 * 
 * @see GameListener
 */
public abstract class GameEvent extends java.util.EventObject {

    /**
     * 
     */
    private static final long serialVersionUID = 8935663731287150831L;
    public static final int GAME_PLAYER_CONNECTED = 0;
    public static final int GAME_PLAYER_DISCONNECTED = 1;
    public static final int GAME_PLAYER_CHANGE = 2;
    public static final int GAME_PLAYER_CHAT = 3;

    public static final int GAME_PHASE_CHANGE = 4;
    public static final int GAME_TURN_CHANGE = 5;
    public static final int GAME_REPORT = 6;
    public static final int GAME_END = 7;

    public static final int GAME_BOARD_NEW = 8;
    public static final int GAME_BOARD_CHANGE = 9;
    public static final int GAME_SETTINGS_CHANGE = 10;
    public static final int GAME_MAP_QUERY = 11;

    public static final int GAME_ENTITY_NEW = 12;
    public static final int GAME_ENTITY_REMOVE = 13;
    public static final int GAME_ENTITY_NEW_OFFBOARD = 14;
    public static final int GAME_ENTITY_CHANGE = 15;
    public static final int GAME_NEW_ACTION = 16;

    protected int type;

    /**
     * Construct game event
     */
    public GameEvent(Object source, int type) {
        super(source);
        this.type = type;
    }

    /**
     * @return the type of the event
     */
    public int getType() {
        return type;
    }

    public String toString() {
        StringBuffer buff = new StringBuffer();
        switch (this.type) {
            case GAME_PLAYER_CHANGE:
                buff.append("Status Change");
                break;
            case GAME_PLAYER_CHAT:
                buff.append("Chat");
                break;
            case GAME_PHASE_CHANGE:
                buff.append("Phase Change");
                break;
            case GAME_TURN_CHANGE:
                buff.append("Turn Change");
                break;
            case GAME_ENTITY_NEW:
                buff.append("New Entities");
                break;
            case GAME_SETTINGS_CHANGE:
                buff.append("New Settings");
                break;
            default:
                buff.append("Unknown");
                break;
        }
        buff.append(" game event ");
        return buff.toString();
    }

}

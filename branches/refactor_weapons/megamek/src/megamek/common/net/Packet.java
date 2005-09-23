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

package megamek.common.net;

/**
 * Application layer data packet used to exchange information between 
 * client and server.
 *
 */
public class Packet {

    public static final int        COMMAND_CLOSE_CONNECTION     = 0;
    public static final int        COMMAND_SERVER_GREETING      = 10;
    public static final int        COMMAND_CLIENT_NAME          = 20;
    public static final int        COMMAND_LOCAL_PN             = 30;
    
    public static final int        COMMAND_PLAYER_ADD           = 40;
    public static final int        COMMAND_PLAYER_REMOVE        = 50;
    public static final int        COMMAND_PLAYER_UPDATE        = 60;
    public static final int        COMMAND_PLAYER_READY         = 70;
    
    public static final int        COMMAND_CHAT                 = 80;
    
    public static final int        COMMAND_ENTITY_ADD           = 90;
    public static final int        COMMAND_ENTITY_REMOVE        = 100;
    public static final int        COMMAND_ENTITY_MOVE          = 110;
    public static final int        COMMAND_ENTITY_DEPLOY        = 120;
    public static final int        COMMAND_ENTITY_ATTACK        = 130;
    public static final int        COMMAND_ENTITY_UPDATE        = 140;
    public static final int        COMMAND_ENTITY_MODECHANGE    = 150;
    public static final int        COMMAND_ENTITY_AMMOCHANGE    = 160;
    public static final int        COMMAND_ENTITY_SYSTEMMODECHANGE = 170;
    
    public static final int        COMMAND_ENTITY_VISIBILITY_INDICATOR = 180;
    public static final int        COMMAND_CHANGE_HEX           = 190;

    public static final int        COMMAND_BLDG_ADD             = 200;
    public static final int        COMMAND_BLDG_REMOVE          = 210;
    public static final int        COMMAND_BLDG_UPDATE_CF       = 220;
    public static final int        COMMAND_BLDG_COLLAPSE        = 230;
    
    public static final int        COMMAND_PHASE_CHANGE         = 240;
    public static final int        COMMAND_TURN                 = 250;
    public static final int        COMMAND_ROUND_UPDATE         = 260;

    public static final int        COMMAND_SENDING_BOARD        = 270;
    public static final int        COMMAND_SENDING_ENTITIES     = 280;
    public static final int        COMMAND_SENDING_PLAYERS      = 290;
    public static final int        COMMAND_SENDING_TURNS        = 300;
    public static final int        COMMAND_SENDING_REPORTS      = 310;
    public static final int        COMMAND_SENDING_REPORTS_SPECIAL= 320;
    public static final int        COMMAND_SENDING_REPORTS_TACTICAL_GENIUS= 330;
    public static final int        COMMAND_SENDING_REPORTS_ALL  = 340;

    public static final int        COMMAND_SENDING_GAME_SETTINGS= 350;
    public static final int        COMMAND_SENDING_MAP_SETTINGS = 360;
    public static final int        COMMAND_QUERY_MAP_SETTINGS   = 370;
                                                                
    public static final int        COMMAND_END_OF_GAME          = 380;
    public static final int        COMMAND_DEPLOY_MINEFIELDS    = 390;
    public static final int        COMMAND_REVEAL_MINEFIELD     = 400;
    public static final int        COMMAND_REMOVE_MINEFIELD     = 410;
    public static final int        COMMAND_SENDING_MINEFIELDS   = 420;

    public static final int        COMMAND_REROLL_INITIATIVE    = 430;    
    public static final int        COMMAND_UNLOAD_STRANDED      = 440;    
    
    public static final int        COMMAND_SET_ARTYAUTOHITHEXES = 450;
    public static final int        COMMAND_SENDING_ARTILLERYATTACKS = 460;
    public static final int        COMMAND_SENDING_FLARES       = 470;

    public static final int        COMMAND_SERVER_CORRECT_NAME  = 480;
    
    public static final int        COMMAND_SEND_SAVEGAME        = 490;

    private int command;

    private Object[] data;
    
    /**
     * Contructs a new Packet with just the command and no
     * data.
     *
     * @param command        the command.
     */
    public Packet(int command) {
        this(command, null);
    }

    /**
     * Creates a <code>Packet</code> with a command and a single object
     * @param command
     * @param object
     */
    public Packet(int command, Object object) {
        this.command = command;
        this.data = new Object[1];
        this.data[0] = object;
    }
    
    /**
     * Creates a <code>Packet</code> with a command and an array of objects
     * @param command
     * @param data
     */
    public Packet(int command, Object[] data) {
        this.command = command;
        this.data = data;
    }
    
    /**
     * Returns the command associated.
     */
    public int getCommand() {
        return command;
    }

    /**
     * Returns the data in the packet
     */
    public Object[] getData() {
        return data;
    }
    
    /**
     * Returns the object at the specified index
     * @param index the index of the desired object
     * @return the object at the specified index
     */
    public Object getObject(int index) {
        if (index >= data.length) {
            return null;
        }
        return data[index];
    }

    /**
     * Returns the <code>int</code> value of the object at the specified index
     * @param index the index of the desired object
     * @return the <code>int</code> value of the object at the specified index
     */
    public int getIntValue(int index) {
        return ((Integer)getObject(index)).intValue();
    }

    /**
     * Returns the <code>boolean</code> value of the object at the specified index
     * @param index the index of the desired object
     * @return the <code>boolean</code> value of the object at the specified index
     */
    public boolean getBooleanValue(int index) {
        return ((Boolean)getObject(index)).booleanValue();
    }
}

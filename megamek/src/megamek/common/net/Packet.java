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
 * Application layer data packet used to exchange information between client and
 * server.
 */
public class Packet {

    public static final int COMMAND_CLOSE_CONNECTION = 0;
    public static final int COMMAND_SERVER_GREETING = 10;
    public static final int COMMAND_CLIENT_NAME = 20;
    public static final int COMMAND_CLIENT_VERSIONS = 25;
    public static final int COMMAND_LOCAL_PN = 30;

    public static final int COMMAND_PLAYER_ADD = 40;
    public static final int COMMAND_PLAYER_REMOVE = 50;
    public static final int COMMAND_PLAYER_UPDATE = 60;
    public static final int COMMAND_PLAYER_READY = 70;

    public static final int COMMAND_CHAT = 80;

    public static final int COMMAND_ENTITY_ADD = 90;
    public static final int COMMAND_ENTITY_REMOVE = 100;
    public static final int COMMAND_ENTITY_MOVE = 110;
    public static final int COMMAND_ENTITY_DEPLOY = 120;
    public static final int COMMAND_ENTITY_DEPLOY_UNLOAD = 121;
    public static final int COMMAND_ENTITY_ATTACK = 130;
    public static final int COMMAND_ENTITY_GTA_HEX_SELECT = 135;
    public static final int COMMAND_ENTITY_UPDATE = 140;
    public static final int COMMAND_ENTITY_WORDER_UPDATE = 145;
    public static final int COMMAND_ENTITY_MODECHANGE = 150;
    public static final int COMMAND_ENTITY_AMMOCHANGE = 160;
    public static final int COMMAND_ENTITY_SENSORCHANGE = 165;
    public static final int COMMAND_ENTITY_SINKSCHANGE = 166;
    public static final int COMMAND_ENTITY_ACTIVATE_HIDDEN = 167;
    public static final int COMMAND_ENTITY_SYSTEMMODECHANGE = 170;

    public static final int COMMAND_ENTITY_VISIBILITY_INDICATOR = 180;
    public static final int COMMAND_ADD_SMOKE_CLOUD = 185;
    public static final int COMMAND_CHANGE_HEX = 190;
    public static final int COMMAND_CHANGE_HEXES= 195;

    public static final int COMMAND_BLDG_ADD = 200;
    public static final int COMMAND_BLDG_REMOVE = 210;
    public static final int COMMAND_BLDG_UPDATE = 220;
    public static final int COMMAND_BLDG_COLLAPSE = 230;
    public static final int COMMAND_BLDG_EXPLODE = 240;

    public static final int COMMAND_PHASE_CHANGE = 240;
    public static final int COMMAND_TURN = 250;
    public static final int COMMAND_ROUND_UPDATE = 260;

    public static final int COMMAND_SENDING_BOARD = 270;
    public static final int COMMAND_SENDING_ILLUM_HEXES = 275;
    public static final int COMMAND_CLEAR_ILLUM_HEXES = 276;
    public static final int COMMAND_SENDING_ENTITIES = 280;
    public static final int COMMAND_SENDING_PLAYERS = 290;
    public static final int COMMAND_SENDING_TURNS = 300;
    public static final int COMMAND_SENDING_REPORTS = 310;
    public static final int COMMAND_SENDING_REPORTS_SPECIAL = 320;
    public static final int COMMAND_SENDING_REPORTS_TACTICAL_GENIUS = 330;
    public static final int COMMAND_SENDING_REPORTS_ALL = 340;

    public static final int COMMAND_SENDING_GAME_SETTINGS = 350;
    public static final int COMMAND_SENDING_MAP_DIMENSIONS = 360;
    public static final int COMMAND_SENDING_MAP_SETTINGS = 370;

    public static final int COMMAND_END_OF_GAME = 380;
    public static final int COMMAND_DEPLOY_MINEFIELDS = 390;
    public static final int COMMAND_REVEAL_MINEFIELD = 400;
    public static final int COMMAND_REMOVE_MINEFIELD = 410;
    public static final int COMMAND_SENDING_MINEFIELDS = 420;
    public static final int COMMAND_UPDATE_MINEFIELDS = 430;

    public static final int COMMAND_REROLL_INITIATIVE = 440;
    public static final int COMMAND_UNLOAD_STRANDED = 450;

    public static final int COMMAND_SET_ARTYAUTOHITHEXES = 460;
    public static final int COMMAND_SENDING_ARTILLERYATTACKS = 470;
    public static final int COMMAND_SENDING_FLARES = 480;

    public static final int COMMAND_SERVER_CORRECT_NAME = 490;

    public static final int COMMAND_SEND_SAVEGAME = 500;
    public static final int COMMAND_LOAD_SAVEGAME = 505;
    public static final int COMMAND_LOAD_GAME = 506;
    public static final int COMMAND_SENDING_SPECIAL_HEX_DISPLAY = 510;
    public static final int COMMAND_SPECIAL_HEX_DISPLAY_APPEND = 511;
    public static final int COMMAND_SPECIAL_HEX_DISPLAY_DELETE = 512;
    public static final int COMMAND_CUSTOM_INITIATIVE = 520;
    public static final int COMMAND_FORWARD_INITIATIVE = 521;
    public static final int COMMAND_SENDING_PLANETARY_CONDITIONS = 530;
    public static final int COMMAND_SQUADRON_ADD = 540;
    public static final int COMMAND_ENTITY_CALLEDSHOTCHANGE = 550;

    public static final int COMMAND_ENTITY_MOUNTED_FACINGCHANGE = 560;

    public static final int COMMAND_SENDING_AVAILABLE_MAP_SIZES = 570;

    public static final int COMMAND_ENTITY_LOAD = 580;
    
    public static final int COMMAND_ENTITY_NOVA_NETWORK_CHANGE = 590;
    
    public static final int COMMAND_RESET_ROUND_DEPLOYMENT = 600;
    
    public static final int COMMAND_SENDING_TAGINFO = 610;
    
    public static final int COMMAND_RESET_TAGINFO = 620;
    
    public static final int COMMAND_CLIENT_FEEDBACK_REQUEST = 700;
    public static final int COMMAND_CFR_DOMINO_EFFECT = 705;
    public static final int COMMAND_CFR_EDGE_PROMPT = 710;
    public static final int COMMAND_CFR_AMS_ASSIGN = 715;
    public static final int COMMAND_CFR_APDS_ASSIGN = 716;
    public static final int COMMAND_CFR_HIDDEN_PBS = 717;
    
    public static final int COMMAND_GAME_VICTORY_EVENT = 800;

    private int command;

    private Object[] data;

    /**
     * Contructs a new Packet with just the command and no data.
     *
     * @param command the command.
     */
    public Packet(int command) {
        this(command, null);
    }

    /**
     * Creates a <code>Packet</code> with a command and a single object
     *
     * @param command
     * @param object
     */
    public Packet(int command, Object object) {
        this.command = command;
        data = new Object[1];
        data[0] = object;
    }

    /**
     * Creates a <code>Packet</code> with a command and an array of objects
     *
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
     *
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
     *
     * @param index the index of the desired object
     * @return the <code>int</code> value of the object at the specified index
     */
    public int getIntValue(int index) {
        return ((Integer) getObject(index)).intValue();
    }

    /**
     * Returns the <code>boolean</code> value of the object at the specified
     * index
     *
     * @param index the index of the desired object
     * @return the <code>boolean</code> value of the object at the specified
     *         index
     */
    public boolean getBooleanValue(int index) {
        return ((Boolean) getObject(index)).booleanValue();
    }
}

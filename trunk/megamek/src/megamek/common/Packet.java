/**
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

package megamek.common;

import java.io.*;

/**
 * Packet has functionality to turn messages into
 * byte arrays and back.
 * 
 * 
 */
public class Packet
  implements Serializable
{
	public static final int		COMMAND_SERVER_NAME		    = 1;
	public static final int		COMMAND_CLIENT_NAME		    = 2;
	public static final int		COMMAND_LOCAL_PN		    = 3;

	public static final int		COMMAND_PLAYER_ADD  	    = 4;
	public static final int		COMMAND_PLAYER_REMOVE       = 5;
	public static final int		COMMAND_PLAYER_UPDATE		= 6;
	public static final int		COMMAND_PLAYER_READY		= 7;
  
	public static final int		COMMAND_CHAT			    = 8;
	
	public static final int		COMMAND_ENTITY_ADD	        = 9;
	public static final int		COMMAND_ENTITY_REMOVE	    = 10;
	public static final int		COMMAND_ENTITY_MOVE		    = 11;
	public static final int		COMMAND_ENTITY_ATTACK       = 12;
	public static final int		COMMAND_ENTITY_READY	    = 13;
	public static final int		COMMAND_ENTITY_UPDATE	    = 14;
	
	public static final int		COMMAND_PHASE_CHANGE	    = 15;
	public static final int		COMMAND_TURN			    = 16;
	
	public static final int		COMMAND_SENDING_BOARD	    = 17;
	public static final int		COMMAND_SENDING_ENTITIES    = 18;
    public static final int     COMMAND_SENDING_PLAYERS     = 19;
	public static final int		COMMAND_SENDING_REPORT	    = 20;
	public static final int		COMMAND_SENDING_GAME_SETTINGS= 21;
	
	
	private int				command;
  private Object[]  data;
	
	/**
	 * Contructs a new Packet with just the command and no
	 * message.
	 * 
	 * @param command		the command.
	 */
	public Packet(int command) {
		this(command, null);
	}
  
  /**
   * Packet with a command and a single object
   */
  public Packet(int command, Object object) {
    this.command = command;
    this.data = new Object[1];
    this.data[0] = object;
  }
  
  /**
   * Packet with a command and an array of objects
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
	 * Returns the object at the specified index
	 */
	public Object getObject(int index) {
		return data[index];
	}
	
	/**
	 * Returns the int value of the object at the specified index
	 */
	public int getIntValue(int index) {
		return ((Integer)data[index]).intValue();
	}
	
	/**
	 * Returns the boolean value of the object at the specified index
	 */
	public boolean getBooleanValue(int index) {
		return ((Boolean)data[index]).booleanValue();
	}
	
	
	
	
}

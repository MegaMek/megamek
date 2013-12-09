/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Nicholas Walczak (walczak@cs.umn.edu)
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

package megamek.client.ui.swing.util;

/**
 * This enum is a collection of commands that can be bound to a particular key.
 * 
 * @author arlith
 *
 */
public enum KeyCommandBind {
	SCROLL_NORTH("scrollN",true),
	SCROLL_SOUTH("scrollS",true),
	SCROLL_EAST("scrollE",true),
	SCROLL_WEST("scrollW",true),
	TOGGLE_CHAT("toggleChat",true),
	ESCAPE_CHAT("escapeChat",true);
	
	public String cmd;
	
	public int key;
	
	public boolean isRepeatable;
	
	private KeyCommandBind(String c, boolean r){
		cmd = c;
		key = -1;
		isRepeatable = r;
	}
	
	public static KeyCommandBind getBindByKey(int keycode){
		for (KeyCommandBind bind : values()){
			if (bind.key == keycode){
				return bind;
			}
		}
		return null;
	}
	
}

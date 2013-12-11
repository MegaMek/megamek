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

import java.util.ArrayList;

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
	TOGGLE_CHAT("toggleChat",false),
	// Change facing one hexside to the left
	TURN_LEFT("turnLeft",false),
	// Change facing one hexside to the right
	TURN_RIGHT("turnRight",false),
	// Change facing one hexside to the left
	TWIST_LEFT("twistLeft",false),
	// Change facing one hexside to the right
	TWIST_RIGHT("twistRight",false),
	// Fire the currently selected weapon
	FIRE("fire", false),
	NEXT_WEAPON("nextWeapon", false),
	PREV_WEAPON("prevWeapon", false),
	NEXT_UNIT("nextUnit", false),
	PREV_UNIT("prevUnit", false),
	NEXT_TARGET("nextTarget", false),
	PREV_TARGET("prevTarget", false),
	// Undo an action, such as a move step in the movement phase
	UNDO("undo",false),
	CENTER_ON_SELECTED("centerOnSelected",false);
	
	/**
	 * The command associated with this binding.
	 */
	public String cmd;
	
	/**
	 * Defines the keycode for the command. This should correspond to defines in
	 * <code>KeyEvent</code>.
	 */
	public int key;
	
	/**
	 * Defines any modifiers to the key code, such as whether control or alt 
	 * are pressed.  This should correspond to the modifiers defined in 
	 * <code>KeyEvent</code>.
	 */
	public int modifiers;
	
	/**
	 * A flag that determines whether this binding is repeatable.  If a bind is
	 * repeatable then when the key is pressed the action will be added to a 
	 * timer and will be repeated until the key is released.
	 */
	public boolean isRepeatable;
	
	private KeyCommandBind(String c, boolean r){
		cmd = c;
		key = -1;
		modifiers = 0;
		isRepeatable = r;
	}
	
	public static ArrayList<KeyCommandBind> getBindByKey(int keycode, int modifiers){
		ArrayList<KeyCommandBind> binds = new ArrayList<KeyCommandBind>();
		for (KeyCommandBind bind : values()){
			if (bind.key == keycode && bind.modifiers == modifiers){
				binds.add(bind);
			}
		}
		return binds;
	}
	
	public static KeyCommandBind getBindByCmd(String cmd){
		for (KeyCommandBind bind : values()){
			if (bind.cmd.equals(cmd)){
				return bind;
			}
		}
		return null;
	}
	
}

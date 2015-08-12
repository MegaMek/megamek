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
	SCROLL_NORTH("scrollN",true, 87, 0), // Default: W
	SCROLL_SOUTH("scrollS",true, 83, 0), // Default: S
	SCROLL_EAST("scrollE",true, 68, 0),  // Default: D
	SCROLL_WEST("scrollW",true, 65, 0),  // Default: A
	// Toggles isometric view on/off
	TOGGLE_ISO("toggleIso",false, 84, 0),// Default: T
	// Activates chat box
    TOGGLE_CHAT("toggleChat",false, 10, 0), // Default: Enter
	// Activates chat box and adds the command character (/)
	TOGGLE_CHAT_CMD("toggleChatCmd", false, 47, 0), // Default: /
	// Change facing one hexside to the left
	TURN_LEFT("turnLeft",false, 65, 1), // Default: Shift-A
	// Change facing one hexside to the right
	TURN_RIGHT("turnRight",false, 68, 1), // Default: Shift-D
	// Change facing one hexside to the left
	TWIST_LEFT("twistLeft",false, 65, 1), // Default: Shift-A
	// Change facing one hexside to the right
	TWIST_RIGHT("twistRight",false, 68, 1), // Default: Shift-D
	// Fire the currently selected weapon
	FIRE("fire", false, 70, 0), // Default: F
	NEXT_WEAPON("nextWeapon", false, 81, 1), // Default: Q
	PREV_WEAPON("prevWeapon", false, 69, 1), // Default: E
	NEXT_UNIT("nextUnit", false, 67, 0), // Default: C
	PREV_UNIT("prevUnit", false, 90, 0), // Default: Z
	NEXT_TARGET("nextTarget", false, 81, 1), // Default: Shift-Q
	PREV_TARGET("prevTarget", false, 69, 1), // Default: Shift-E
	// Undo an action, such as a move step in the movement phase
	UNDO("undo",false, 8, 0), // Default: Backspace
	MOVE_ENVELOPE("movementEnvelope",false, 82, 0), // Default: R
	CENTER_ON_SELECTED("centerOnSelected",false, 32, 0), // Default: Space
	AUTO_ARTY_DEPLOYMENT_ZONE("autoArtyDeployZone",false, 90, 1), // Default: Shift-Z
	FIELD_FIRE("fieldOfFire",false, 82, 1); // Default: Shift-R
	
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
	
	private KeyCommandBind(String c, boolean r, int k, int m){
		cmd = c;
		key = k;
		modifiers = m;
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

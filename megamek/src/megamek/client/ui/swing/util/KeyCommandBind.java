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

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

/**
 * This enum is a collection of commands that can be bound to a particular key.
 * 
 * @author arlith
 *
 */
public enum KeyCommandBind {
    SCROLL_NORTH("scrollN",true, KeyEvent.VK_W, 0), // Default: W
    SCROLL_SOUTH("scrollS",true, KeyEvent.VK_S, 0), // Default: S
    SCROLL_EAST("scrollE",true, KeyEvent.VK_D, 0),  // Default: D
    SCROLL_WEST("scrollW",true, KeyEvent.VK_A, 0),  // Default: A
    // Toggles isometric view on/off
    TOGGLE_ISO("toggleIso",false, KeyEvent.VK_T, 0),// Default: T
    // Activates chat box
    TOGGLE_CHAT("toggleChat",false, KeyEvent.VK_ENTER, 0), // Default: Enter
    // Activates chat box and adds the command character (/)
    TOGGLE_CHAT_CMD("toggleChatCmd", false, KeyEvent.VK_SLASH, 0), // Default: /
    // Change facing one hexside to the left
    TURN_LEFT("turnLeft",false, KeyEvent.VK_A, InputEvent.SHIFT_MASK), // Default: Shift-A
    // Change facing one hexside to the right
    TURN_RIGHT("turnRight",false, KeyEvent.VK_D, InputEvent.SHIFT_MASK), // Default: Shift-D
    // Change facing one hexside to the left
    TWIST_LEFT("twistLeft",false, KeyEvent.VK_A, InputEvent.SHIFT_MASK), // Default: Shift-A
    // Change facing one hexside to the right
    TWIST_RIGHT("twistRight",false, KeyEvent.VK_D, InputEvent.SHIFT_MASK), // Default: Shift-D
    // Fire the currently selected weapon
    FIRE("fire", false, KeyEvent.VK_F, 0), // Default: F
    NEXT_WEAPON("nextWeapon", false, KeyEvent.VK_E, 0), // Default: E
    PREV_WEAPON("prevWeapon", false, KeyEvent.VK_Q, 0), // Default: Q
    NEXT_UNIT("nextUnit", false, KeyEvent.VK_E, InputEvent.SHIFT_MASK), // Default: Shift-E
    PREV_UNIT("prevUnit", false, KeyEvent.VK_Q, InputEvent.SHIFT_MASK), // Default: Shift0Q
    NEXT_TARGET("nextTarget", false, KeyEvent.VK_C, 0), // Default: C
    PREV_TARGET("prevTarget", false, KeyEvent.VK_Z, 0), // Default: Z
    NEXT_TARGET_VALID("nextTargetValid", false, KeyEvent.VK_C, InputEvent.SHIFT_MASK), // Default: Shift-C
    PREV_TARGET_VALID("prevTargetValid", false, KeyEvent.VK_Z, InputEvent.SHIFT_MASK), // Default: Shift-Z
    NEXT_TARGET_NOALLIES("nextTargetNoAllies", false, KeyEvent.VK_C, InputEvent.CTRL_MASK), // Default: Ctrl-C
    PREV_TARGET_NOALLIES("prevTargetNoAllies", false, KeyEvent.VK_Z, InputEvent.CTRL_MASK), // Default: Ctrl-Z
    NEXT_TARGET_VALID_NO_ALLIES("nextTargetValidNoAllies", false, KeyEvent.VK_C, InputEvent.CTRL_MASK & InputEvent.SHIFT_MASK), // Default: Ctrl-Shift-C
    PREV_TARGET_VALID_NO_ALLIES("prevTargetValidNoAllies", false, KeyEvent.VK_Z, InputEvent.CTRL_MASK & InputEvent.SHIFT_MASK), // Default: Ctrl-Shift-Z
    // Undo an action, such as a move step in the movement phase
    UNDO("undo",false, KeyEvent.VK_BACK_SPACE, 0), // Default: Backspace
    MOVE_ENVELOPE("movementEnvelope",false, KeyEvent.VK_R, 0), // Default: R
    CENTER_ON_SELECTED("centerOnSelected",false, KeyEvent.VK_SPACE, 0), // Default: Space
    AUTO_ARTY_DEPLOYMENT_ZONE("autoArtyDeployZone",false, KeyEvent.VK_Z, InputEvent.SHIFT_MASK), // Default: Shift-Z
    FIELD_FIRE("fieldOfFire",false, KeyEvent.VK_R, InputEvent.SHIFT_MASK), // Default: Shift-R
    // Used to cancel moves/fires/chatterbox
    CANCEL("cancel", false, KeyEvent.VK_ESCAPE, 0, true), // Default: Escape
    DONE("done", false, KeyEvent.VK_ENTER, InputEvent.CTRL_MASK, true), // Default: Ctrl-Enter
    // Used to select the tab in the unit display
    UD_GENERAL("udGeneral", false, KeyEvent.VK_F1, 0), // Default: F1
    UD_PILOT("udPilot", false, KeyEvent.VK_F2, 0), // Default: F2
    UD_ARMOR("udArmor", false, KeyEvent.VK_F3, 0), // Default: F3
    UD_SYSTEMS("udSystems", false, KeyEvent.VK_F4, 0), // Default: F4
    UD_WEAPONS("udWeapons", false, KeyEvent.VK_F5, 0), // Default: F5
    UD_EXTRAS("udExtras", false, KeyEvent.VK_F6, 0), // Default: F6
    /** Toggles between Jumping and Walk/Run, also acts as a reset when a unit cannot jump */
    TOGGLE_MOVEMODE("toggleJump", false, KeyEvent.VK_J, 0), // Default: J
    PREV_MODE("prevMode", false, KeyEvent.VK_TAB, InputEvent.CTRL_MASK), // Default: Tab
    NEXT_MODE("nextMode", false, KeyEvent.VK_TAB, 0), // Default: Tab
    TOGGLE_DRAW_LABELS("toggleDrawLabels", false, KeyEvent.VK_Y, 0); // Default: Y


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
     * Defines if an action is exclusive, which means that only one
     * CommandAction will be performed for each key press.  The CommandAction
     * that is performed will be the first one encountered.
     */
    public boolean isExclusive = false;

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
    
    private KeyCommandBind(String c, boolean r, int k, int m, boolean e){
        cmd = c;
        key = k;
        modifiers = m;
        isRepeatable = r;
        isExclusive = e;
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

/*
 * Copyright (c) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2013 Nicholas Walczak (walczak@cs.umn.edu)
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing.util;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This enum is a collection of commands that can be bound to a particular key.
 * @author arlith
 */
public enum KeyCommandBind {
    SCROLL_NORTH("scrollN", true, KeyEvent.VK_W), 
    SCROLL_SOUTH("scrollS", true, KeyEvent.VK_S), 
    SCROLL_EAST("scrollE", true, KeyEvent.VK_D),  
    SCROLL_WEST("scrollW", true, KeyEvent.VK_A),  
    // Toggles isometric view on/off
    TOGGLE_ISO("toggleIso", false, KeyEvent.VK_T),
    // Activates chat box
    TOGGLE_CHAT("toggleChat", false, KeyEvent.VK_BACK_QUOTE), // Default: ` (back quote/grave)
    // Activates chat box and adds the command character (/)
    TOGGLE_CHAT_CMD("toggleChatCmd", false, KeyEvent.VK_SLASH),
    // Change facing one hexside to the left
    TURN_LEFT("turnLeft", false, KeyEvent.VK_A, InputEvent.SHIFT_DOWN_MASK),
    // Change facing one hexside to the right
    TURN_RIGHT("turnRight", false, KeyEvent.VK_D, InputEvent.SHIFT_DOWN_MASK),
    // Change facing one hexside to the left
    TWIST_LEFT("twistLeft", false, KeyEvent.VK_A, InputEvent.SHIFT_DOWN_MASK),
    // Change facing one hexside to the right
    TWIST_RIGHT("twistRight", false, KeyEvent.VK_D, InputEvent.SHIFT_DOWN_MASK),
    // Fire the currently selected weapon
    FIRE("fire", false, KeyEvent.VK_F),
    NEXT_WEAPON("nextWeapon", false, KeyEvent.VK_E),
    PREV_WEAPON("prevWeapon", false, KeyEvent.VK_Q),
    NEXT_UNIT("nextUnit", false, KeyEvent.VK_E, InputEvent.SHIFT_DOWN_MASK),
    PREV_UNIT("prevUnit", false, KeyEvent.VK_Q, InputEvent.SHIFT_DOWN_MASK),
    NEXT_TARGET("nextTarget", false, KeyEvent.VK_C),
    PREV_TARGET("prevTarget", false, KeyEvent.VK_Z),
    NEXT_TARGET_VALID("nextTargetValid", false, KeyEvent.VK_C, InputEvent.SHIFT_DOWN_MASK),
    PREV_TARGET_VALID("prevTargetValid", false, KeyEvent.VK_Z, InputEvent.SHIFT_DOWN_MASK),
    NEXT_TARGET_NOALLIES("nextTargetNoAllies", false, KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK),
    PREV_TARGET_NOALLIES("prevTargetNoAllies", false, KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK),
    NEXT_TARGET_VALID_NO_ALLIES("nextTargetValidNoAllies", false, KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK & InputEvent.SHIFT_DOWN_MASK),
    PREV_TARGET_VALID_NO_ALLIES("prevTargetValidNoAllies", false, KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK & InputEvent.SHIFT_DOWN_MASK),
    // Undo an action, such as a move step in the movement phase
    UNDO("undo", false, KeyEvent.VK_BACK_SPACE),
    MOVE_ENVELOPE("movementEnvelope", false, KeyEvent.VK_R),
    CENTER_ON_SELECTED("centerOnSelected", false, KeyEvent.VK_SPACE),
    AUTO_ARTY_DEPLOYMENT_ZONE("autoArtyDeployZone", false, KeyEvent.VK_Z, InputEvent.SHIFT_DOWN_MASK),
    FIELD_FIRE("fieldOfFire", false, KeyEvent.VK_R, InputEvent.SHIFT_DOWN_MASK),
    // Used to cancel moves/fires/chatterbox
    CANCEL("cancel", false, KeyEvent.VK_ESCAPE, 0, true),
    DONE("done", false, KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK, true),
    // Used to select the tab in the unit display
    UD_GENERAL("udGeneral", false, KeyEvent.VK_F1),
    UD_PILOT("udPilot", false, KeyEvent.VK_F2),
    UD_ARMOR("udArmor", false, KeyEvent.VK_F3),
    UD_SYSTEMS("udSystems", false, KeyEvent.VK_F4),
    UD_WEAPONS("udWeapons", false, KeyEvent.VK_F5),
    UD_EXTRAS("udExtras", false, KeyEvent.VK_F6),
    /** Toggles between Jumping and Walk/Run, also acts as a reset when a unit cannot jump */
    TOGGLE_MOVEMODE("toggleJump", false, KeyEvent.VK_J),
    TOGGLE_CONVERSIONMODE("toggleConversion", false, KeyEvent.VK_M),
    PREV_MODE("prevMode", false, KeyEvent.VK_TAB, InputEvent.CTRL_DOWN_MASK),
    NEXT_MODE("nextMode", false, KeyEvent.VK_TAB),
    TOGGLE_DRAW_LABELS("toggleDrawLabels", false, KeyEvent.VK_Y),
    TOGGLE_KEYBIND_DISPLAY("toggleKeyBindDisplay", false, KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK),
    TOGGLE_HEX_COORDS("toggleHexCoords", false, KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK);

    /** The command associated with this binding. */
    public String cmd;
    
    /** Defines the keycode for the command, e.g. KeyEvent.VK_X. */
    public int key;
    
    /** Modifiers to the key code, such as InputEvent.CTRL_DOWN_MASK. */ 
    public int modifiers;
    
    /**
     * Defines if an action is exclusive, which means that only one
     * CommandAction will be performed for each key press. The CommandAction
     * that is performed will be the first one encountered.
     */
    public boolean isExclusive = false;

    /**
     * For a repeatable bind, when the key is pressed the action will be added to a 
     * timer and repeated until the key is released.
     */
    public boolean isRepeatable;
    
    private KeyCommandBind(String c, boolean r, int k){
        this(c, r, k, 0, false);
    }
    
    private KeyCommandBind(String c, boolean r, int k, int m){
        this(c, r, k, m, false);
    }
    
    private KeyCommandBind(String c, boolean r, int k, int m, boolean e){
        cmd = c;
        key = k;
        modifiers = m;
        isRepeatable = r;
        isExclusive = e;
    }

    /** Returns a list of binds using the given keycode and modifier. */
    public static List<KeyCommandBind> getBindByKey(int keycode, int modifiers) {
        return Stream.of(values())
                .filter(bind -> bind.key == keycode)
                .filter(bind -> bind.modifiers == modifiers)
                .collect(Collectors.toList());
    }
    
    /** Returns the bind identified by the given cmd or null if there is no such bind. */
    public static KeyCommandBind getBindByCmd(String cmd) {
        return Stream.of(values()).filter(bind -> bind.cmd.equals(cmd)).findAny().orElse(null);
    }
    
}
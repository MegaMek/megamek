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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.KeyStroke;

import static java.awt.event.KeyEvent.*;

/**
 * This enum is a collection of commands that can be bound to a particular key.
 * @author arlith
 */
public enum KeyCommandBind {

    SCROLL_NORTH("scrollN", true, VK_W),
    SCROLL_SOUTH("scrollS", true, VK_S),
    SCROLL_EAST("scrollE", true, VK_D),
    SCROLL_WEST("scrollW", true, VK_A),
    // Activates chat box
    TOGGLE_CHAT("toggleChat", VK_ENTER),
    // Activates chat box and adds the command character (/)
    TOGGLE_CHAT_CMD("toggleChatCmd", VK_SLASH),
    // Change facing one hexside to the left
    TURN_LEFT("turnLeft", VK_A, SHIFT_DOWN_MASK),
    // Change facing one hexside to the right
    TURN_RIGHT("turnRight", VK_D, SHIFT_DOWN_MASK),
    // Change facing one hexside to the left
    TWIST_LEFT("twistLeft", VK_A, SHIFT_DOWN_MASK),
    // Change facing one hexside to the right
    TWIST_RIGHT("twistRight", VK_D, SHIFT_DOWN_MASK),
    // Fire the currently selected weapon
    FIRE("fire", VK_F),
    NEXT_WEAPON("nextWeapon", VK_DOWN),
    PREV_WEAPON("prevWeapon", VK_UP),
    NEXT_UNIT("nextUnit", VK_TAB),
    PREV_UNIT("prevUnit", VK_TAB, SHIFT_DOWN_MASK),
    NEXT_TARGET("nextTarget", VK_RIGHT),
    PREV_TARGET("prevTarget", VK_LEFT),
    NEXT_TARGET_VALID("nextTargetValid", VK_RIGHT, SHIFT_DOWN_MASK),
    PREV_TARGET_VALID("prevTargetValid", VK_LEFT, SHIFT_DOWN_MASK),
    NEXT_TARGET_NOALLIES("nextTargetNoAllies", VK_RIGHT, CTRL_DOWN_MASK),
    PREV_TARGET_NOALLIES("prevTargetNoAllies", VK_LEFT, CTRL_DOWN_MASK),
    NEXT_TARGET_VALID_NO_ALLIES("nextTargetValidNoAllies", VK_RIGHT, CTRL_DOWN_MASK | SHIFT_DOWN_MASK),
    PREV_TARGET_VALID_NO_ALLIES("prevTargetValidNoAllies", VK_LEFT, CTRL_DOWN_MASK | SHIFT_DOWN_MASK),
    // Changes unit display to view acting unit and sets current viewed unit as target
    VIEW_ACTING_UNIT("viewActingUnit", VK_V),
    // Remove the last move step or the last added weapon fire
    UNDO_LAST_STEP("undoLastStep", VK_BACK_SPACE),
    // General Undo and Redo, e.g. for the Board Editor
    UNDO("undo", VK_Z, CTRL_DOWN_MASK),
    REDO("redo", VK_Y, CTRL_DOWN_MASK),
    /** Center on the currently selected unit. */
    CENTER_ON_SELECTED("centerOnSelected", VK_SPACE),
    AUTO_ARTY_DEPLOYMENT_ZONE("autoArtyDeployZone", VK_Z, SHIFT_DOWN_MASK),
    /** Used to cancel moves/fires/chatterbox */
    CANCEL("cancel", false, VK_ESCAPE, 0, true),
    DONE("done", false, VK_ENTER, CTRL_DOWN_MASK, true),
    DONE_NO_ACTION("doneNoAction", false, VK_ENTER, CTRL_DOWN_MASK | SHIFT_DOWN_MASK, true),
    // Used to select the tab in the unit display
    UD_GENERAL("udGeneral", VK_F1),
    UD_PILOT("udPilot", VK_F2),
    UD_ARMOR("udArmor", VK_F3),
    UD_SYSTEMS("udSystems", VK_F4),
    UD_WEAPONS("udWeapons", VK_F5),
    UD_EXTRAS("udExtras", VK_F6),
    /** Toggles between Jumping and Walk/Run, also acts as a reset when a unit cannot jump */
    TOGGLE_MOVEMODE("toggleJump", VK_J),
    TOGGLE_CONVERSIONMODE("toggleConversion", VK_M),
    PREV_MODE("prevMode", VK_KP_DOWN),
    NEXT_MODE("nextMode", VK_KP_UP),

    // --------- The following binds are used by the CommonMenuBar:
    // Toggles isometric view on/off
    TOGGLE_ISO(true, "toggleIso", VK_T),
    MOVE_ENVELOPE(true, "movementEnvelope", VK_Q, CTRL_DOWN_MASK),
    FIELD_FIRE(true, "fieldOfFire", VK_R),
    DRAW_LABELS(true, "toggleDrawLabels", VK_B, CTRL_DOWN_MASK),
    HEX_COORDS(true, "toggleHexCoords", VK_G, CTRL_DOWN_MASK),
    MINIMAP(true, "toggleMinimap", VK_M, CTRL_DOWN_MASK),
    LOS_SETTING(true, "viewLosSetting", VK_L, CTRL_DOWN_MASK | ALT_DOWN_MASK),
    UNIT_DISPLAY(true, "toggleUnitDisplay", VK_D, CTRL_DOWN_MASK),
    UNIT_OVERVIEW(true, "toggleUnitOverview", VK_U, CTRL_DOWN_MASK),
    KEY_BINDS(true, "toggleKeybinds", VK_K, CTRL_DOWN_MASK),
    PLANETARY_CONDITIONS(true, "togglePlanetaryConditions", VK_P, CTRL_DOWN_MASK),
    TURN_DETAILS(true, "toggleTurnDetails", VK_T, CTRL_DOWN_MASK),
    CLIENT_SETTINGS(true, "clientSettings", VK_C, ALT_DOWN_MASK),
    INC_GUISCALE(true, "incGuiScale", VK_ADD, CTRL_DOWN_MASK),
    DEC_GUISCALE(true, "decGuiScale", VK_SUBTRACT, CTRL_DOWN_MASK),
    ROUND_REPORT(true, "roundReport", VK_R, CTRL_DOWN_MASK),
    ZOOM_IN(true, "zoomIn", VK_ADD),
    ZOOM_OUT(true, "zoomOut", VK_SUBTRACT),
    QUICK_LOAD(true, "quickLoad", VK_L, CTRL_DOWN_MASK | SHIFT_DOWN_MASK),
    QUICK_SAVE(true, "quickSave", VK_S, CTRL_DOWN_MASK | SHIFT_DOWN_MASK),
    LOCAL_LOAD(true, "localLoad", VK_L, CTRL_DOWN_MASK),
    LOCAL_SAVE(true, "localSave", VK_S, CTRL_DOWN_MASK),
    REPLACE_PLAYER(true, "replacePlayer", VK_R, CTRL_DOWN_MASK | SHIFT_DOWN_MASK),
    MOD_ENVELOPE(true, "viewModEnvelope", VK_W, CTRL_DOWN_MASK),
    SENSOR_RANGE(true, "sensorRange", VK_C),
    UNDO_SINGLE_STEP("undoSingleStep", VK_BACK_SPACE, CTRL_DOWN_MASK),
    FORCE_DISPLAY(true, "toggleForceDisplay", VK_F, CTRL_DOWN_MASK),
    EXTEND_TURN_TIMER("extendTurnTimer", VK_F4, CTRL_DOWN_MASK);

    /** The command associated with this binding. */
    public String cmd;

    /** Defines the keycode for the command, e.g. KeyEvent.VK_X. */
    public int key;
    public int keyDefault;

    /** Modifiers to the key code, such as InputEvent.CTRL_DOWN_MASK. */
    public int modifiers;
    public int modifiersDefault;

    /**
     * Defines if an action is exclusive, which means that only one
     * CommandAction will be performed for each key press. The CommandAction
     * that is performed will be the first one encountered. Exclusive binds can't share their key
     * with other binds.
     */
    public boolean isExclusive = false;

    /**
     * For a repeatable bind, when the key is pressed the action will be added to a
     * timer and repeated until the key is released.
     */
    public boolean isRepeatable;

    /**
     * When true, this keybind is used by the CommonMenuBar. Binding it with the MegaMekController like the other
     * keybinds is not necessary (and will not work, as the command will not be recognized in this way).
     */
    public boolean isMenuBar = false;

    private KeyCommandBind(String c, int k) {
        this(c, false, k, 0, false);
    }

    private KeyCommandBind(String c, int k, int m) {
        this(c, false, k, m, false);
    }

    private KeyCommandBind(String c, boolean r, int k) {
        this(c, r, k, 0, false);
    }

    private KeyCommandBind(String c, boolean r, int k, int m) {
        this(c, r, k, m, false);
    }

    // CommonMenuBar keybinds - these are exclusive, as multiple menu items on the same key don't work
    private KeyCommandBind(boolean n, String c, int k, int m) {
        this(c, false, k, m, true);
        isMenuBar = n;
    }

    private KeyCommandBind(boolean n, String c, int k) {
        this(c, false, k, 0, true);
        isMenuBar = n;
    }

    private KeyCommandBind(String c, boolean r, int k, int m, boolean e) {
        cmd = c;
        key = k;
        keyDefault = k;
        modifiers = m;
        modifiersDefault = m;
        isRepeatable = r;
        isExclusive = e;
    }

    /**
     * Returns a list of binds using the given keycode and modifier. Only lists those
     * that are not used by the CommonMenuBar!
     */
    public static List<KeyCommandBind> getBindByKey(int keycode, int modifiers) {
        return Stream.of(values())
                .filter(bind -> !bind.isMenuBar)
                .filter(bind -> bind.key == keycode)
                .filter(bind -> bind.modifiers == modifiers)
                .collect(Collectors.toList());
    }

    /** Returns the bind identified by the given cmd or null if there is no such bind. */
    public static KeyCommandBind getBindByCmd(String cmd) {
        return Stream.of(values()).filter(bind -> bind.cmd.equals(cmd)).findAny().orElse(null);
    }

    /** Returns a KeyStroke for a given KeyCommandBind. */
    public static KeyStroke keyStroke(KeyCommandBind bind) {
        return KeyStroke.getKeyStroke(bind.key, bind.modifiers);
    }

    /** returns formatted mod + key for display*/
    public static String getDesc(KeyCommandBind k) {
        String mod = getModifiersExText(k.modifiers);
        String key = getKeyText(k.key);
        return (mod.isEmpty() ? "" : mod + "+") + key;
    }
}
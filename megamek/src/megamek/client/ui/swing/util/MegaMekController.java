/*
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
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

import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;
import java.util.*;
import megamek.client.ui.swing.*;

/**
 * This class implements a KeyEventDispatcher, which handles all generated
 * KeyEvents. If the KeyEvent correspondes to a registerd hotkey, the action for
 * that hotkey will be used to consume the event otherwise the event will be
 * dispatched as normal.
 *
 * The idea is that the system is split into two: keys can be bound to string
 * commands, and string commands can be bound to CommandAction
 * objects, which are a simple class that implements an "actionPerformed"
 * method. The class that implements the CommandAction creates the
 * object and registers it, agnostic to what key is bound to the command. Then,
 * somewhere else (ie; a file) can specify what keys are bound to what string
 * commands. The possible string commands are specified in
 * KeyCommandBind.
 *
 * There are three things that need to be done to create a key binding. First, a
 * command must exist, defined in KeyCommandBind. Then, the command
 * must be bound to a key in the keybind XML file (mmconf/defaultKeyBinds.xml by
 * default). Finally, a CommandAction needs to be registered
 * somewhere.
 *
 * @author arlith
 */
public class MegaMekController implements KeyEventDispatcher {

    private static final int MAX_REPEAT_RATE = 100;

    public BoardEditor boardEditor = null;
    public ClientGUI clientgui = null;

    /** Maps a key code to a command string. */
    protected Set<KeyCommandBind> keyCmdSet;

    /** Maps command strings to CommandAction objects. */
    protected Map<String, ArrayList<CommandAction>> cmdActionMap;

    /**
     * Timer for repeating commands for key presses. This is necessary to
     * override the default key repeat delay.
     */
    protected Timer keyRepeatTimer;

    /** Keeps track of the tasks that are currently repeating. */
    protected Map<KeyCommandBind, TimerTask> repeatingTasks;

    /** Should we ignore key presses? */
    protected boolean ignoreKeyPresses = false;

    public MegaMekController() {
        keyCmdSet = new HashSet<>();
        cmdActionMap = new HashMap<>();
        keyRepeatTimer = new Timer("Key Repeat Timer");
        repeatingTasks = new HashMap<>();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent evt) {

        // Don't consider hotkeys when the clientgui has a dialog visible
        if (((clientgui != null) && clientgui.shouldIgnoreHotKeys())
                || ((boardEditor != null) && boardEditor.shouldIgnoreHotKeys())
                || ignoreKeyPresses) {
            return false;
        }

        int keyCode = evt.getKeyCode();
        int modifiers = evt.getModifiersEx();

        // If there's no action associated with this key bind, or the
        // current action is invalid, do not consume this event.
        boolean consumed = false;

        for (var kcb : KeyCommandBind.getBindByKey(keyCode, modifiers)) {
            // Do nothing when there is no bind for this key or no action for the bind
            if (!keyCmdSet.contains(kcb) || (cmdActionMap.get(kcb.cmd) == null)) {
                continue;
            }

            for (var action : cmdActionMap.get(kcb.cmd)) {
                // If the action is null or shouldn't be performed, skip it
                if ((action == null) || !action.shouldPerformAction()) {
                    continue;
                }
                // If we perform at least one action, this event is consumed
                consumed = true;

                if (evt.getID() == KeyEvent.KEY_PRESSED) {
                    if (kcb.isRepeatable) {
                        startRepeating(kcb, action);
                    } else {
                        action.performAction();
                        // Exclusive actions are only performed once
                        if (kcb.isExclusive) {
                            break; // Stop processing the rest of the actions
                        }
                    }
                }

                if (evt.getID() == KeyEvent.KEY_RELEASED) {
                    // If the key bind is repeatable, we need to stop its timer
                    if (kcb.isRepeatable) {
                        stopRepeating(kcb);
                    }
                    if (action.hasReleaseAction()) {
                        action.releaseAction();
                    }
                }
            }
        }
        // If we had a binding, this event should be considered consumed
        return consumed;
    }

    public synchronized void registerKeyCommandBind(KeyCommandBind kcb) {
        keyCmdSet.add(kcb);
    }

    public synchronized void removeAllKeyCommandBinds() {
        keyCmdSet.clear();
    }

    public synchronized void registerCommandAction(String cmd,
            CommandAction action) {
        ArrayList<CommandAction> actions = cmdActionMap.get(cmd);
        if (actions == null) {
            actions = new ArrayList<>();
            actions.add(action);
            cmdActionMap.put(cmd, actions);
        } else {
            actions.add(action);
        }
    }

    public synchronized void removeAllActions() {
        for (ArrayList<CommandAction> actions : cmdActionMap.values()) {
            actions.clear();
        }
    }

    /**
     * Start a new repeating timer task for the given KeyCommandBind. If the given 
     * KeyCommandBind already has a repeating task, a new one is not added. Also, 
     * if there is no mapped CommandAction for the given KeyCommandBind no task is scheduled.
     */
    protected void startRepeating(KeyCommandBind kcb, final CommandAction action) {

        GUIPreferences guip = GUIPreferences.getInstance();
        // Make sure the delay is positive
        long delay = Math.max(0, guip.getInt(GUIPreferences.ADVANCED_KEY_REPEAT_DELAY));
        // Make sure the rate is positive and that it is below a maximum
        int rate = Math.max(0, Math.min(MAX_REPEAT_RATE, guip.getInt(GUIPreferences.ADVANCED_KEY_REPEAT_RATE)));
        long period = (long) (1000.0 / rate);

        // If we're already repeating, don't add a new task
        if (repeatingTasks.containsKey(kcb)) {
            return;
        }

        // Get the corresponding action, stop if there's no mapped action
        if (action == null) {
            return;
        }

        TimerTask tt = new TimerTask() {
            // Should only be executed by keyRepeatTimer thread.
            @Override
            public void run() {
                action.performAction();

                // Attempt to make it more responsive to key-releases.
                // Even if there are multiple this-tasks piled up (due to
                // "scheduleAtFixedRate") we don't want this thread to take
                // precedence over AWT thread.
                Thread.yield();
            }
        };
        repeatingTasks.put(kcb, tt);
        keyRepeatTimer.scheduleAtFixedRate(tt, delay, period);
    }

    /** Stops the repeat timer task for the given KeyCommandBind. */
    public void stopRepeating(KeyCommandBind kcb) {
        if (repeatingTasks.containsKey(kcb)) {
            repeatingTasks.get(kcb).cancel();
            repeatingTasks.remove(kcb);
        }
    }

    /** Stop all repeat timers. */
    public void stopAllRepeating() {
        for (KeyCommandBind kcb : repeatingTasks.keySet()) {
            repeatingTasks.get(kcb).cancel();
            repeatingTasks.remove(kcb);
        }
    }

    /** Set whether key presses should be ignored or not. */
    public void setIgnoreKeyPresses(boolean ignoreKeyPresses) {
        this.ignoreKeyPresses = ignoreKeyPresses;
    }

}

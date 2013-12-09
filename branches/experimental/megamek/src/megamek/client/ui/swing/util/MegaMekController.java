/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
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

import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;


/**
 * This class implements a KeyEventDispatcher, which handles all generated 
 * KeyEvents.  If the KeyEvent correspondes to a registerd hotkey, the action 
 * for that hotkey will be used to consume the event otherwise the event will
 * be dispatched as normal.
 * 
 * The idea is that the system is split into two: keys can be bound to string
 * commands, and string commands can be bound to <code>CommandAction</code> 
 * objects, which are a simple class that implements an "actionPerformed" 
 * method.  The class that implements the <code>CommandAction</code> creates 
 * the object and registers it, agnostic to what key is bound to the command.  
 * Then, somewhere else (ie; a file) can specify what keys are bound to what 
 * string commands.  The possible string commands are specified in 
 * <code>KeyCommandBind</code>.
 *  
 * @author arlith
 *
 */
public class MegaMekController implements KeyEventDispatcher {

	private static final int MAX_REPEAT_RATE = 10;
    private static final int MAX_REPEAT_DELAY = 10;
	
	/**
	 * Map that maps a key code to a command string.
	 */
	protected Set<KeyCommandBind> keyCmdSet;
	
	/**
	 * Map that maps command strings to CommandAction objects.
	 */
	protected Map<String, CommandAction> cmdActionMap; 
	
	/**
	 * Timer for repeating commands for key presses.  This is necessary to 
	 * override the default key repeat delay.
	 */
	protected Timer keyRepeatTimer;
    
    /**
     * Map that keeps track of the tasks that are currently repeating
     */
	protected Map<KeyCommandBind, TimerTask> repeatingTasks;
	
	public MegaMekController(){	
		keyCmdSet = new HashSet<KeyCommandBind>();
		cmdActionMap = new HashMap<String, CommandAction>();
		keyRepeatTimer = new Timer("Key Repeat Timer");
		repeatingTasks = new HashMap<KeyCommandBind, TimerTask>();
	}
	
	@Override
	public boolean dispatchKeyEvent(KeyEvent evt) {
		int keyCode = evt.getKeyCode();
		KeyCommandBind kcb = KeyCommandBind.getBindByKey(keyCode);
		
		// Do we have a binding for this key?
		if (!keyCmdSet.contains(kcb)){
			return false;
		}
		
		// If there's no action associated with this key bind, or the currenty
		//  action is invalid, do not consume this event.
		CommandAction action = cmdActionMap.get(kcb.cmd);
		if (action == null || !action.shouldPerformAction()){
			return false;
		}
		
		if (evt.getID() == KeyEvent.KEY_PRESSED) {
			if (kcb.isRepeatable){
				startRepeating(kcb, action);
			} else {
					action.performAction();
			}
		}
		
		// If the key bind is repeatable, we need to stop it's timer event
		if (evt.getID() == KeyEvent.KEY_RELEASED && kcb.isRepeatable) {
			stopRepeating(kcb);
		}
		
		// If we had a binding, this event should be considered consumed
		return true;
	}
	
	public void registerKeyCommandBind(KeyCommandBind kcb){
		keyCmdSet.add(kcb);		
	}
	
	public void registerCommandAction(String cmd, CommandAction action){
		cmdActionMap.put(cmd,action);
	}
	
	/**
	 * Start a new repeating timer task for the given 
	 * <code>KeyCommandBind</code>.  If the given <code>KeyCommandBind</code>
	 * already has a repeating task, a new one is not added.  Also, if there is
	 * no mapped <code>CommandAction<c/code> for the given 
	 * <code>KeyCommandBind</code> no task is scheduled.
	 * @param kcb
	 */
	protected void startRepeating(KeyCommandBind kcb, 
			final CommandAction action){
		
		long delay = MAX_REPEAT_DELAY;
		int rate = MAX_REPEAT_RATE;
		long period = (long) (1000.0 / rate);
		
		// If we're already repeating, don't add a new task
		if (repeatingTasks.containsKey(kcb)){
			return;
		}
		
		// Get the corresponding actoin, stop if there's no mapped action
		if (action == null){
			return;
		}
		
		TimerTask tt = new TimerTask() {
            // Should only be executed by keyRepeatTimer thread.
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
	
	public void stopRepeating(KeyCommandBind kcb){
		// If we're not repeating, there's nothing to cancel
		if (!repeatingTasks.containsKey(kcb)){
			return;
		}
		repeatingTasks.get(kcb).cancel();
        repeatingTasks.remove(kcb);
	}

}
